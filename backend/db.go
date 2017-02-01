package backend

import (
	"errors"
	"strconv"
	"time"

	"golang.org/x/net/context"
	"google.golang.org/appengine/datastore"
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
	if err != nil {
		return err
	}

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
	ss := []Stats{}

	ks, err := datastore.NewQuery("Stats").
		Limit(1).
		Order("-Timestamp").
		GetAll(ds.ctx, &ss)
	if err != nil {
		return Stats{}, err
	}

	if len(ss) == 0 {
		return Stats{}, ErrNoStats
	}

	s := ss[0]
	k := ks[0]

	var vs []Vote
	vks, err := datastore.NewQuery("Vote").
		Ancestor(k).
		GetAll(ds.ctx, &vs)
	if err != nil {
		return Stats{}, err
	}

	s.Votes = make(map[string]Vote)
	for i, v := range vs {
		s.Votes[vks[i].StringID()] = v
	}

	return s, nil
}

// ForEachFrom calls the given method for each saves Block starting from the
// given Timestamp
func (ds *Datastore) ForEachFrom(ts time.Time, f func(Block)) error {
	t := datastore.NewQuery("Block").
		Order("-Timestamp").
		Filter("Timestamp > ", ts).
		Run(ds.ctx)

	for {
		var b Block
		_, err := t.Next(&b)
		f(b)
		if err == datastore.Done {
			break
		}
		if err != nil {
			return err
		}
	}

	return nil
}
