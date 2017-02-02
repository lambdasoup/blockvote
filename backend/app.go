package backend

import (
	"encoding/json"
	"net/http"
	"time"

	"google.golang.org/appengine"
	"google.golang.org/appengine/log"
)

func init() {
	http.HandleFunc("/poll", pollFunc)
	http.HandleFunc("/update-stats", updateStatsFunc)
	http.HandleFunc("/stats", statsFunc)
}

func statsFunc(w http.ResponseWriter, r *http.Request) {
	ctx := appengine.NewContext(r)

	db := &Datastore{ctx}
	logger := &AELogger{ctx}

	be := &Backend{db, nil, logger, nil}

	s, err := be.latestStats()
	if err != nil {
		log.Errorf(ctx, err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
		return
	}

	b, err := json.Marshal(s)
	if err != nil {
		log.Errorf(ctx, "cannot marshal %v", s)
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
		return
	}
	w.Write(b)
}

func updateStatsFunc(w http.ResponseWriter, r *http.Request) {
	ctx := appengine.NewContext(r)

	db := &Datastore{ctx}
	provider := &Blockcypher{ctx}
	logger := &AELogger{ctx}
	reactor := &AEReactor{ctx}

	b := &Backend{db, provider, logger, reactor}

	err := b.updateStats(time.Now())
	if err != nil {
		log.Errorf(ctx, err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
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
		log.Errorf(ctx, err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
	}
}
