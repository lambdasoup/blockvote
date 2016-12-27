package backend

import (
	"errors"
	"strconv"

	"golang.org/x/net/context"
	"google.golang.org/appengine/datastore"
)

// Errors
var (
	ErrNoBlocks = errors.New("no blocks available")
)

// Datastore implements DB via Google Cloud Datastore
type Datastore struct {
}

// DB is an interface for the known blocks
type DB interface {
	// Save saves the given block into the database
	Save(context.Context, Block) error

	// GetConfig gets the current config from the database
	GetConfig(context.Context) (Config, error)

	// Latest returns the latest block (by height) in the database
	Latest(context.Context) (Block, error)
}

// Save saves the given block into the database
func (ds *Datastore) Save(ctx context.Context, b Block) error {
	k := datastore.NewKey(ctx, "Block", "block-"+strconv.Itoa(b.Height), 0, nil)
	_, err := datastore.Put(ctx, k, &b)
	return err
}

// GetConfig gets the current config from the database
func (ds *Datastore) GetConfig(ctx context.Context) (c Config, err error) {
	k := datastore.NewKey(ctx, "Config", "config", 0, nil)
	err = datastore.Get(ctx, k, &c)
	if err == datastore.ErrNoSuchEntity {
		_, err = datastore.Put(ctx, k, &c)
	}
	return c, err
}

// Latest returns the latest block (by height) in the database
func (ds *Datastore) Latest(ctx context.Context) (b Block, err error) {
	// run query
	bs := []Block{}
	_, err = datastore.NewQuery("Block").
		Limit(1).
		Order("-Height").
		GetAll(ctx, &bs)

	if err != nil {
		return
	}

	if len(bs) == 0 {
		return b, ErrNoBlocks
	}

	b = bs[0]
	return
}
