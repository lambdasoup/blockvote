package backend

import (
	"errors"
	"strconv"
	"time"

	"golang.org/x/net/context"
	"google.golang.org/appengine/datastore"
	"google.golang.org/appengine/log"
)

// Errors
var (
	ErrNoBlocks = errors.New("no blocks available")
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

	// Latest returns the latest block (by height) in the database
	Latest() (Block, error)

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
			log.Infof(tctx, "saving vote: %v", s)
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

// Latest returns the latest block (by height) in the database
func (ds *Datastore) Latest() (b Block, err error) {
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
