package backend

import (
	"testing"
	"time"
)

type TestDB struct {
	blocks []Block
	stats  Stats
}

func (db *TestDB) GetConfig() (Config, error) {
	return Config{}, nil
}

func (db *TestDB) LatestBlock() (Block, error) {
	return Block{}, nil
}

func (db *TestDB) LatestStats() (Stats, error) {
	return Stats{}, nil
}

func (db *TestDB) GetStats(n int) ([]Stats, error) {
	return nil, nil
}

func (db *TestDB) SaveBlock(Block) error {
	return nil
}

func (db *TestDB) SaveStats(s Stats) error {
	db.stats = s
	return nil
}

func (db *TestDB) ForEachFrom(ts time.Time, f func(Block)) error {
	for _, b := range db.blocks {
		f(b)
	}
	return nil
}

type TestProvider struct{}

func (p *TestProvider) Fetch(h int) (Block, error) {
	return Block{}, nil
}

type TestLogger struct{}

func (l *TestLogger) Infof(format string, args ...interface{}) {

}

func (l *TestLogger) Errorf(format string, args ...interface{}) {

}

type TestReactor struct{}

func (r *TestReactor) TriggerUpdateStats() error {
	return nil
}

func (r *TestReactor) SendFCMMessage(key string, topic string, s Stats) error {
	return nil
}

func Test_Poll(t *testing.T) {
	be := &Backend{&TestDB{}, &TestProvider{}, &TestLogger{}, &TestReactor{}}
	err := be.poll()
	if err != nil {
		t.Errorf("poll failed: %v", err)
	}
}

func Test_makeVote(t *testing.T) {
	cs := []int{8, 2, 1}
	total := []int{10, 10, 10}
	v := makeVote(cs, total)
	if v.D30 != 0.8 {
		t.Errorf("D30 should be 0.8: %v", v)
	}
	if v.D7 != 0.2 {
		t.Errorf("D7 should be 0.2: %v", v)
	}
	if v.D1 != 0.1 {
		t.Errorf("D1 should be 0.1: %v", v)
	}
}

func Test_Stats(t *testing.T) {
	ts := time.Now()

	b1 := Block{Version: 0x20000002, Timestamp: ts.Add(-day * 20), Script: "0391a106102f5669614254432f4542322f4144342f2cfabe6d6dad259f24295e0872f4771fff38a6d80b3cdb359cb28dcc9009521c0899a2a5500100000000000000122d36fd1028f01e12b6c48886fda23a0e0a00"}
	b2 := Block{Version: 0x20000002, Timestamp: ts.Add(-day * 20)}
	b3 := Block{Version: 0x20000000, Timestamp: ts.Add(-day * 2)}
	b4 := Block{Version: 0x20000000, Timestamp: ts, Script: "0391a106102f5669614254432f4542322f4144342f2cfabe6d6dad259f24295e0872f4771fff38a6d80b3cdb359cb28dcc9009521c0899a2a5500100000000000000122d36fd1028f01e12b6c48886fda23a0e0a00"}
	db := &TestDB{blocks: []Block{b1, b2, b3, b4}}

	be := &Backend{db, nil, &TestLogger{}, &TestReactor{}}
	err := be.updateStats(ts)
	if err != nil {
		t.Fatalf("stats failed: %v", err)
	}

	if db.stats.Timestamp.Day() != ts.Day() {
		t.Errorf("wrong timestamp in saved stats")
	}

	if len(db.stats.Votes) != 2 {
		t.Errorf("wrong vote count: %v", db.stats)
	}

	if db.stats.Votes["segwit"].D30 != 0.5 {
		t.Errorf("wrong segwit D30 count: %v", db.stats)
	}

}

var (
	unlimited = Block{
		Script:  "0391a106102f5669614254432f4542322f4144342f2cfabe6d6dad259f24295e0872f4771fff38a6d80b3cdb359cb28dcc9009521c0899a2a5500100000000000000122d36fd1028f01e12b6c48886fda23a0e0a00",
		Version: 0x20000000,
	}
	segwit = Block{
		Script:  "035fa106134742534233503169c11db2b3a4fc14580305dc1001000029b58f00",
		Version: 0x20000002,
	}
	none = Block{
		Script:  "035fa106134742534233503169c11db2b3a4fc14580305dc1001000029b58f00",
		Version: 0x20000000,
	}
	both = Block{
		Script:  "0391a106102f5669614254432f4542322f4144342f2cfabe6d6dad259f24295e0872f4771fff38a6d80b3cdb359cb28dcc9009521c0899a2a5500100000000000000122d36fd1028f01e12b6c48886fda23a0e0a00",
		Version: 0x20000002,
	}
)

func TestHasSegWitSignal(tt *testing.T) {
	tcs := []struct {
		block    Block
		expected bool
	}{
		{unlimited, false},
		{segwit, true},
		{none, false},
		{both, true},
	}

	for _, tc := range tcs {
		if hasSWSignal(tc.block) != tc.expected {
			tt.Error("segwit test failed")
		}
	}
}

func TestHasUnlimitedSignal(tt *testing.T) {
	tcs := []struct {
		block    Block
		expected bool
	}{
		{unlimited, true},
		{segwit, false},
		{none, false},
		{both, true},
	}

	for _, tc := range tcs {
		if hasECSignal(tc.block) != tc.expected {
			tt.Error("unlimited test failed")
		}
	}
}
