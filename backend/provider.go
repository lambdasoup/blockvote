package backend

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"google.golang.org/appengine/urlfetch"

	"golang.org/x/net/context"
)

// Package specific errors
var (
	ErrBlockNotFound = errors.New("no block found")
)

// Provider provides Blockhain info
type Provider interface {
	// Fetch fetches a block at the given height
	Fetch(context.Context, int) (Block, error)
}

// Blockcypher is a Blockchain info provider hosted at blockcypher.com
type Blockcypher struct {
}

// Fetch fetches a Block at the given height
func (bc *Blockcypher) Fetch(ctx context.Context, h int) (Block, error) {
	client := urlfetch.Client(ctx)

	// get block
	res, err := client.Get("https://api.blockcypher.com/v1/btc/main/blocks/" + strconv.Itoa(h) + "?txstart=0&limit=1")
	if err != nil {
		return Block{}, err
	}
	if res.StatusCode == http.StatusNotFound {
		return Block{}, ErrBlockNotFound
	}
	br := struct {
		Version   int       `json:"ver"`
		Height    int       `json:"height"`
		Hash      string    `json:"hash"`
		Timestamp time.Time `json:"time"`
		TxURL     string    `json:"tx_url"`
		TxIDs     []string  `json:"txids"`
	}{}
	err = json.NewDecoder(res.Body).Decode(&br)
	if err != nil {
		return Block{}, fmt.Errorf("could not parse body: %v", err)
	}

	// get first tx
	res, err = client.Get(br.TxURL + "/" + br.TxIDs[0])
	if res.StatusCode == http.StatusNotFound {
		return Block{}, fmt.Errorf("could not find first transaction: %v", err)
	}
	tr := struct {
		Inputs []struct {
			Script string `json:"script"`
		} `json:"inputs"`
	}{}
	err = json.NewDecoder(res.Body).Decode(&tr)
	if err != nil {
		return Block{}, fmt.Errorf("could not parse body: %v", err)
	}

	b := Block{}
	b.Script = tr.Inputs[0].Script
	b.Hash = br.Hash
	b.Height = br.Height
	b.Timestamp = br.Timestamp
	b.Version = br.Version

	return b, nil
}
