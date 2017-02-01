package backend

import (
	"golang.org/x/net/context"
	"google.golang.org/appengine/taskqueue"
)

// Reactor is a backend service which triggers follow-up events
type Reactor interface {
	// TriggerUpdateStats triggers a stats update
	TriggerUpdateStats() error
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
