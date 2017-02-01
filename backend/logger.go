package backend

import (
	"golang.org/x/net/context"
	"google.golang.org/appengine/log"
)

// Logger logs stuff
type Logger interface {
	Errorf(format string, args ...interface{})
	Infof(format string, args ...interface{})
}

// AELogger is the App Engine logger
type AELogger struct {
	ctx context.Context
}

// Errorf logs an error
func (ael *AELogger) Errorf(format string, args ...interface{}) {
	log.Errorf(ael.ctx, format, args)
}

// Infof logs an error
func (ael *AELogger) Infof(format string, args ...interface{}) {
	log.Infof(ael.ctx, format, args)
}
