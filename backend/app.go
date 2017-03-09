package backend

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/lambdasoup/blockvote/service"

	"google.golang.org/appengine"
	"google.golang.org/appengine/log"
)

func init() {
	http.HandleFunc("/poll", pollFunc)
	http.HandleFunc("/update-stats", updateStatsFunc)
	http.HandleFunc("/stats", statsFunc)
	http.HandleFunc("/stats-conversation", statsConversationFunc)
	http.HandleFunc("/history", historyFunc)
}

// ConversationResponse is the response type for API.AI webhooks
type ConversationResponse struct {
	Speech      string `json:"speech"`
	DisplayText string `json:"displaytext"`
	Source      string `json:"source"`
}

func historyFunc(w http.ResponseWriter, r *http.Request) {
	ctx := appengine.NewContext(r)

	db := &Datastore{ctx}
	logger := &AELogger{ctx}

	be := &Backend{db, nil, logger, nil}
	ss, err := be.history()
	if err != nil {
		log.Errorf(ctx, err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
		return
	}

	h := &service.History{}
	for _, s := range ss {
		hs := &service.Stats{Votes: makeProtoVotes(s.Votes), Time: s.Timestamp.Format(time.RFC3339)}
		h.Stats = append(h.Stats, hs)
	}

	b, err := proto.Marshal(h)
	if err != nil {
		log.Errorf(ctx, "cannot marshal %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
		return
	}
	w.Header().Add("cache-control", "max-age=3600")
	w.Write(b)
}

func makeProtoVotes(xs map[string]Vote) map[string]*service.Vote {
	ys := make(map[string]*service.Vote)
	for k, v := range xs {
		ys[k] = &service.Vote{D1: v.D1, D7: v.D7, D30: v.D30}
	}
	return ys
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

func statsConversationFunc(w http.ResponseWriter, r *http.Request) {
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

	fsSW := "During the last 24 hours, Segregated Witness was in the lead with %d%% and Bitcoin Unlimited had %d%%"
	fsBU := "During the last 24 hours, Bitcoin Unlimited was in the lead with %d%% and Segregated Witness had %d%%"
	fsEQ := "During the last 24 hours, Bitcoin Unlimited and Segregated Witness both had %d%%"

	var fs string
	d1SW := int(s.Votes["segwit"].D1*100 + 0.5)
	d1BU := int(s.Votes["unlimited"].D1*100 + 0.5)
	if d1SW > d1BU {
		fs = fmt.Sprintf(fsSW, d1SW, d1BU)
	} else if d1BU > d1SW {
		fs = fmt.Sprintf(fsBU, d1BU, d1SW)
	} else {
		fs = fmt.Sprintf(fsEQ, d1BU)
	}

	cr := ConversationResponse{
		Source:      "Block Vote",
		Speech:      fs,
		DisplayText: fs,
	}

	b, err := json.Marshal(cr)
	if err != nil {
		log.Errorf(ctx, "cannot marshal %v", s)
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(err.Error()))
		return
	}
	w.Header().Set("content-type", "application/json")
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
