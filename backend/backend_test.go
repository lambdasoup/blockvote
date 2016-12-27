package backend

import (
	"testing"

	"golang.org/x/net/context"
)

type TestProvider struct{}
type TestDB struct{}

func (tp *TestProvider) Fetch(_ context.Context, _ int) (b Block, err error) {
	return
}

func (tdb *TestDB) GetConfig(_ context.Context) (c Config, err error) {
	return
}

func (tdb *TestDB) Latest(_ context.Context) (b Block, err error) {
	return
}

func (tdb *TestDB) Save(_ context.Context, _ Block) error {
	return nil
}

func Test_Poll(t *testing.T) {
	provider = &TestProvider{}
	db = &TestDB{}
}
