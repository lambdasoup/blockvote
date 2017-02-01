package backend

import (
	"net/http"
	"time"

	"google.golang.org/appengine"
)

func init() {
	http.HandleFunc("/poll", pollFunc)
	http.HandleFunc("/stats", statsFunc)
}

func statsFunc(w http.ResponseWriter, r *http.Request) {
	ctx := appengine.NewContext(r)

	db := &Datastore{ctx}
	provider := &Blockcypher{ctx}
	logger := &AELogger{ctx}
	reactor := &AEReactor{ctx}

	b := &Backend{db, provider, logger, reactor}

	err := b.stats(time.Now())
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("err " + err.Error()))
	}
}

func pollFunc(w http.ResponseWriter, r *http.Request) {
	ctx := appengine.NewContext(r)

	db := &Datastore{ctx}
	provider := &Blockcypher{ctx}
	logger := &AELogger{ctx}
	reactor := &AEReactor{ctx}

	b := &Backend{db, provider, logger, reactor}

	err := b.poll()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte("err " + err.Error()))
	}
}
