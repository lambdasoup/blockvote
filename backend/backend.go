package backend

import (
	"net/http"
	"strconv"
	"time"

	"google.golang.org/appengine"
	"google.golang.org/appengine/log"
)

var provider Provider
var db DB

func init() {
	provider = &Blockcypher{}
	db = &Datastore{}

	http.HandleFunc("/poll", pollFunc)
}

// Config is the backend's config
type Config struct {
	// InitialHeight is the block height where polling should start with
	InitialHeight int
}

// Block is one Blockchain entry
type Block struct {
	Height    int       `json:"height"`
	Hash      string    `json:"hash"`
	Version   int       `json:"version"`
	Script    string    `json:"script"`
	Timestamp time.Time `json:"time"`
}

func pollFunc(w http.ResponseWriter, r *http.Request) {
	ctx := appengine.NewContext(r)

	// TODO determine next block height
	var h int
	lb, err := db.Latest(ctx)
	switch err {
	case ErrNoBlocks:
		// height from config
		var c Config
		c, err = db.GetConfig(ctx)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			w.Write([]byte("err " + err.Error()))
			return
		}
		h = c.InitialHeight
	case nil:
		// height from latest block
		h = lb.Height + 1
	default:
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("err " + err.Error()))
		return
	}

	// fetch next block
	b, err := provider.Fetch(ctx, h)
	switch err {
	default:
		log.Errorf(ctx, "could not fetch block: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("could not fetch block:" + err.Error()))
		return
	case ErrBlockNotFound:
		w.Write([]byte("block " + strconv.Itoa(h) + " not available yet"))
		return
	case nil:
		// happy case
		break
	}

	err = db.Save(ctx, b)
	if err != nil {
		log.Errorf(ctx, "could not save block: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("could not save block: " + err.Error()))
		return
	}
	log.Infof(ctx, "fetched and saved block %v", b)
	w.Write([]byte("fetched and saved block " + strconv.Itoa(b.Height)))
}
