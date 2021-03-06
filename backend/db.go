package backend

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"time"

	"golang.org/x/net/context"
	"google.golang.org/appengine"
	"google.golang.org/appengine/datastore"
	"google.golang.org/appengine/log"
	"google.golang.org/appengine/memcache"
)

// Errors
var (
	ErrNoBlocks = errors.New("no blocks available")
	ErrNoStats  = errors.New("no stats available")
)

// Datastore implements DB via Google Cloud Datastore
type Datastore struct {
	ctx context.Context
}

// DB is an interface for the known blocks
type DB interface {
	// SaveBlock saves the given block into the database
	SaveBlock(Block) error

	// GetConfig gets the current config from the database
	GetConfig() (Config, error)

	// LatestBlock returns the latest block (by height) in the database
	LatestBlock() (Block, error)

	// LatestStats returns the latest stats in the database
	LatestStats() (Stats, error)

	// GetStats returns the (up to) n latest stats
	GetStats(int) ([]Stats, error)

	// SaveStats saves the given stats into the db
	SaveStats(Stats) error

	// ForEachFrom calls the given method for each saves Block starting from the
	// given Timestamp
	ForEachFrom(ts time.Time, f func(Block)) error
}

// SaveBlock saves the given block into the database
func (ds *Datastore) SaveBlock(b Block) error {
	k := datastore.NewKey(ds.ctx, "Block", "block-"+strconv.Itoa(b.Height), 0, nil)
	_, err := datastore.Put(ds.ctx, k, &b)
	return err
}

func (ds *Datastore) dayStatsKey(t time.Time) *datastore.Key {
	return datastore.NewKey(ds.ctx, "Stats", "stats-"+t.UTC().Format("2006-01-02"), 0, nil)
}

// SaveStats saves the given stats into the db
func (ds *Datastore) SaveStats(s Stats) error {
	log.Infof(ds.ctx, "saving stats %v", s)
	err := datastore.RunInTransaction(ds.ctx, func(tctx context.Context) error {
		k := ds.dayStatsKey(s.Timestamp)
		_, terr := datastore.Put(tctx, k, &s)
		if terr != nil {
			return terr
		}

		for sv, v := range s.Votes {
			vk := datastore.NewKey(tctx, "Vote", sv, 0, k)
			_, terr := datastore.Put(tctx, vk, &v)
			if terr != nil {
				return terr
			}
		}

		return nil
	}, nil)
	return err
}

// GetConfig gets the current config from the database
func (ds *Datastore) GetConfig() (c Config, err error) {
	k := datastore.NewKey(ds.ctx, "Config", "config", 0, nil)
	err = datastore.Get(ds.ctx, k, &c)
	if err == datastore.ErrNoSuchEntity {
		_, err = datastore.Put(ds.ctx, k, &c)
	}
	return c, err
}

// LatestBlock returns the latest block (by height) in the database
func (ds *Datastore) LatestBlock() (b Block, err error) {
	// run query
	bs := []Block{}
	_, err = datastore.NewQuery("Block").
		Limit(1).
		Order("-Height").
		GetAll(ds.ctx, &bs)

	if err != nil {
		return
	}

	if len(bs) == 0 {
		return b, ErrNoBlocks
	}

	b = bs[0]
	return
}

// LatestStats returns the latest stats in the database
func (ds *Datastore) LatestStats() (Stats, error) {
	ss, err := ds.GetStats(1)

	if err != nil {
		return Stats{}, err
	}

	return ss[0], nil
}

// GetStats returns the n latest stats
func (ds *Datastore) GetStats(n int) ([]Stats, error) {
	ss := []Stats{}

	// get stats/ancestor keys
	ks, err := datastore.NewQuery("Stats").
		Limit(n).
		Order("-Timestamp").
		GetAll(ds.ctx, &ss)
	if err != nil {
		return nil, err
	}

	if len(ss) == 0 {
		return nil, ErrNoStats
	}

	// create list of vote/child keys (sw0, bu0, sw1, bu1,...)
	vks := []*datastore.Key{}
	for _, k := range ks {
		swk := datastore.NewKey(ds.ctx, "Vote", "segwit", 0, k)
		s2xk := datastore.NewKey(ds.ctx, "Vote", "s2x", 0, k)
		eck := datastore.NewKey(ds.ctx, "Vote", "ec", 0, k)
		vks = append(vks, swk, eck, s2xk)
	}

	// get child values (sw0, bu0, sw1, bu1,...)
	vs := make([]Vote, len(vks))
	err = datastore.GetMulti(ds.ctx, vks, vs)
	switch err.(type) {
	case appengine.MultiError:
		break
	default:
		return nil, err
	}

	// fill stats with child data
	for i := range ss {
		ss[i].Votes = make(map[string]Vote)
		ss[i].Votes["segwit"] = vs[3*i]
		ss[i].Votes["ec"] = vs[3*i+1]
		ss[i].Votes["s2x"] = vs[3*i+2]
		ss[i].Votes["unlimited"] = ss[i].Votes["ec"]
	}

	return ss, nil
}

// ForEachFrom calls the given method for each saves Block starting from the
// given Timestamp
func (ds *Datastore) ForEachFrom(ts time.Time, f func(Block)) error {
	// get all block keys for period
	t := datastore.NewQuery("Block").
		Filter("Timestamp >=", ts).
		KeysOnly().
		Run(ds.ctx)
	var bks []*datastore.Key
	c := 0
	for {
		c++
		bk, err := t.Next(nil)
		if err == datastore.Done {
			break
		}
		if err != nil {
			return fmt.Errorf("error after iterating over %d block keys: %v", c, err)
		}
		bks = append(bks, bk)
	}
	log.Infof(ds.ctx, "iterated over %d keys", c)

	// iterate over blocks
	for _, bk := range bks {
		b, err := ds.getBlock(bk)
		if err != nil {
			return err
		}
		f(b)
	}

	return nil
}

func (ds *Datastore) getBlock(bk *datastore.Key) (Block, error) {
	var b Block

	item, err := memcache.Get(ds.ctx, bk.StringID())

	// cache miss
	if err == memcache.ErrCacheMiss {

		// get from DB
		err = datastore.Get(ds.ctx, bk, &b)
		if err != nil {
			return b, err
		}

		// encode for memcache
		var v []byte
		v, err = json.Marshal(&b)
		if err != nil {
			return b, err
		}
		item = &memcache.Item{
			Key:   bk.StringID(),
			Value: v,
		}

		// set to memcache
		err = memcache.Set(ds.ctx, item)
		if err != nil {
			return b, err
		}

		return b, nil
	}

	// something else went wrong
	if err != nil {
		return b, err
	}

	err = json.Unmarshal(item.Value, &b)
	return b, err
}
