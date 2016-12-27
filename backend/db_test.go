package backend

import (
	"testing"

	"google.golang.org/appengine"
	"google.golang.org/appengine/aetest"
	"google.golang.org/appengine/datastore"
)

func Test_GetConfig_OK(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	ctx, done, _ := aetest.NewContext()
	defer done()

	// put config into db
	k := datastore.NewKey(ctx, "Config", "config", 0, nil)
	c := Config{InitialHeight: 200000}
	datastore.Put(ctx, k, &c)

	db := Datastore{}
	c, err := db.GetConfig(ctx)

	if err != nil {
		t.Fatal(err)
	}

	if c.InitialHeight != 200000 {
		t.Fatal("wrong config returned")
	}
}

func Test_GetConfig_NoConfig(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	ctx, done, _ := aetest.NewContext()
	defer done()

	db := Datastore{}
	c, err := db.GetConfig(ctx)

	if err != nil {
		t.Fatal(err)
	}

	if c.InitialHeight != 0 {
		t.Fatal("wrong config initialized")
	}
}

func Test_Save(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	ctx, done, _ := aetest.NewContext()
	defer done()

	db := Datastore{}
	b := Block{}
	err := db.Save(ctx, b)

	if err != nil {
		t.Fatal(err)
	}
}

func Test_Latest_OK(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	inst, _ := aetest.NewInstance(&aetest.Options{StronglyConsistentDatastore: true})
	r, _ := inst.NewRequest("GET", "", nil)
	ctx := appengine.NewContext(r)
	defer inst.Close()

	db := Datastore{}
	b1 := Block{Height: 200000}
	b2 := Block{Height: 200001}
	db.Save(ctx, b1)
	db.Save(ctx, b2)

	b, err := db.Latest(ctx)

	if err != nil {
		t.Fatal(err)
	}

	if b.Height != 200001 {
		t.Fatal("wrong block returned as latest")
	}
}

func Test_Latest_ErrNoBlocks(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping test in short mode.")
	}

	ctx, done, _ := aetest.NewContext()
	defer done()

	db := Datastore{}

	_, err := db.Latest(ctx)

	if err != ErrNoBlocks {
		t.Fatal("should have no blocks error")
	}
}
