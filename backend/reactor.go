package backend

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"

	"golang.org/x/net/context"
	"google.golang.org/appengine/taskqueue"
	"google.golang.org/appengine/urlfetch"
)

// FCM implements a Firebase Clound Messaging Client
type FCM struct {
	Topic string
	Key   string
}

// FCMMessage is an FCM Message
type FCMMessage struct {
	To   string `json:"to"`
	Data Stats  `json:"data"`
}

// Reactor is a backend service which triggers follow-up events
type Reactor interface {
	// TriggerUpdateStats triggers a stats update
	TriggerUpdateStats() error

	// SendFCMMessage send an FCM message to clients
	SendFCMMessage(string, string, Stats) error
}

// AEReactor is a Reactor for Appengine
type AEReactor struct {
	ctx context.Context
}

// TriggerUpdateStats triggers a stats update
func (r *AEReactor) TriggerUpdateStats() error {
	t := taskqueue.NewPOSTTask("/update-stats", nil)
	_, err := taskqueue.Add(r.ctx, t, "")
	return err
}

// SendFCMMessage send an FCM message to clients
func (r *AEReactor) SendFCMMessage(key string, topic string, s Stats) error {
	if key == "" {
		return errors.New("no FCM key availavble")
	}

	client := urlfetch.Client(r.ctx)
	m := FCMMessage{To: "/topics/" + topic, Data: s}
	log.Printf("sending body %v", m)
	bs, err := json.Marshal(m)
	if err != nil {
		return fmt.Errorf("could make FCM body: %v", err)
	}
	req, err := http.NewRequest("POST", "https://fcm.googleapis.com/fcm/send", bytes.NewReader(bs))
	if err != nil {
		return err
	}

	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("Authorization", "key="+key)

	res, err := client.Do(req)
	if err != nil {
		return err
	}

	body := struct {
		MsgID int    `json:"message_id"`
		Err   string `json:"error"`
	}{}
	err = json.NewDecoder(res.Body).Decode(&body)
	if err != nil {
		return err
	}

	return nil
}
