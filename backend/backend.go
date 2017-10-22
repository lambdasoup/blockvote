package backend

import (
	"bytes"
	"encoding/hex"
	"fmt"
	"time"
)

// Config is the backend's config
type Config struct {
	// InitialHeight is the block height where polling should start with
	InitialHeight int

	// FCMTopic is the FCM topic we are sending to
	FCMTopic string

	// FCMKey is the FCM server key we are using
	FCMKey string
}

var (
	signalAD  = []byte("AD")
	signalEB  = []byte("EB")
	signalS2X = []byte("NYA")
)

// Block is one Blockchain entry
type Block struct {
	Height    int       `json:"height"`
	Hash      string    `json:"hash"`
	Version   int       `json:"version"`
	Script    string    `json:"script"`
	Timestamp time.Time `json:"time"`
}

// Stats is the statitics for one day
type Stats struct {
	Timestamp time.Time       `json:"time"`
	Votes     map[string]Vote `datastore:"-" json:"votes"`
}

// Vote is a the block vote for muliple intervals
type Vote struct {
	D1  float32 `json:"d1"`
	D7  float32 `json:"d7"`
	D30 float32 `json:"d30"`
}

// Backend is the backend here
type Backend struct {
	DB
	Provider
	Logger
	Reactor
}

var day = time.Hour * 24

func (be *Backend) latestStats() (Stats, error) {
	return be.LatestStats()
}

func (be *Backend) history() ([]Stats, error) {
	return be.GetStats(30)
}

func (be *Backend) updateStats(ts time.Time) error {
	s := Stats{Timestamp: ts, Votes: make(map[string]Vote)}

	d30ts := ts.Add(-day * 30)
	d7ts := ts.Add(-day * 7)
	d1ts := ts.Add(-day)

	total := make([]int, 3)
	swcs := make([]int, 3)
	eccs := make([]int, 3)
	s2xcs := make([]int, 3)

	err := be.ForEachFrom(d30ts, func(b Block) {
		total[0]++
		if hasSWSignal(b) {
			swcs[0]++
		}
		if hasECSignal(b) {
			eccs[0]++
		}
		if hasS2XSignal(b) {
			s2xcs[0]++
		}

		if b.Timestamp.After(d7ts) {
			total[1]++
			if hasSWSignal(b) {
				swcs[1]++
			}
			if hasECSignal(b) {
				eccs[1]++
			}
			if hasS2XSignal(b) {
				s2xcs[1]++
			}
		}

		if b.Timestamp.After(d1ts) {
			total[2]++
			if hasSWSignal(b) {
				swcs[2]++
			}
			if hasECSignal(b) {
				eccs[2]++
			}
			if hasS2XSignal(b) {
				s2xcs[2]++
			}
		}
	})
	if err != nil {
		return err
	}

	s.Votes["segwit"] = makeVote(swcs, total)
	s.Votes["ec"] = makeVote(eccs, total)
	s.Votes["s2x"] = makeVote(s2xcs, total)
	s.Votes["unlimited"] = s.Votes["ec"]

	err = be.SaveStats(s)
	if err != nil {
		return err
	}

	c, err := be.GetConfig()
	if err != nil {
		return err
	}

	err = be.SendFCMMessage(c.FCMKey, c.FCMTopic, s)

	return err
}

func hasSWSignal(b Block) bool {
	return (0x00000002 & b.Version) > 0
}

func hasECSignal(b Block) bool {
	bs, err := hex.DecodeString(b.Script)
	if err != nil {
		panic(fmt.Sprintf("could not decode script in block %v", b))
	}
	return bytes.Contains(bs, signalAD) && bytes.Contains(bs, signalEB)
}

func hasS2XSignal(b Block) bool {
	bs, err := hex.DecodeString(b.Script)
	if err != nil {
		panic(fmt.Sprintf("could not decode script in block %v", b))
	}
	return bytes.Contains(bs, signalS2X)
}

func makeVote(cs []int, total []int) Vote {
	return Vote{
		D30: float32(cs[0]) / float32(total[0]),
		D7:  float32(cs[1]) / float32(total[1]),
		D1:  float32(cs[2]) / float32(total[2]),
	}
}

func (be *Backend) poll() error {
	// determine next block height
	var h int
	lb, err := be.LatestBlock()
	switch err {
	case ErrNoBlocks:
		// height from config
		var c Config
		c, err = be.GetConfig()
		if err != nil {
			return err
		}
		h = c.InitialHeight
	case nil:
		// height from latest block
		h = lb.Height + 1
	default:
		return err
	}

	// fetch next block
	b, err := be.Fetch(h)
	switch err {
	default:
		be.Errorf("could not fetch block: %v", err)
		return err
	case ErrBlockNotFound:
		be.Infof("block %d not available yet", h)
		return nil
	case nil:
		// happy case
		break
	}

	err = be.SaveBlock(b)
	if err != nil {
		be.Errorf("could not save block: %v", err)
		return err
	}
	be.Infof("fetched and saved block %v", b)
	return err
}
