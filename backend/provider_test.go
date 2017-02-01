package backend

import (
	"testing"

	"google.golang.org/appengine/aetest"
)

func Test_Fetch_OK(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	ctx, done, _ := aetest.NewContext()
	defer done()

	bc := Blockcypher{ctx}
	b, err := bc.Fetch(10000)

	if err != nil {
		t.Fatal(err)
	}

	if b.Height != 10000 {
		t.Fatal("wrong block fetched")
	}
}

func Test_Fetch_NotFound(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	ctx, done, _ := aetest.NewContext()
	defer done()

	bc := Blockcypher{ctx}
	_, err := bc.Fetch(1000000)

	if err != ErrBlockNotFound {
		t.Fatal(err)
	}
}
