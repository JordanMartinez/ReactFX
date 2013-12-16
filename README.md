InhiBeans
=========

When there is a network of bound values, it often happens that a single user action on one end of the network results in a succession of changes of a value on the other end of the network. Most of the time redundant invalidation and change events do not cause problems, but they can come with a performance penalty if the attached listeners eagerly perform expensive computations. InhiBeans help inhibit this invalidation madness.


API
---

InhiBeans extend classes from `javafx.beans.property` and `javafx.beans.binding` and add these methods:

```java
interface InhibitoryObservableValue<T> extends ObservableValue<T>, AutoCloseable {
    AutoCloseable block();
    void release();
    void close();
    void blockWhile(Runnable);
}
```

The `block()` method prevents notification of invalidation and change listeners.

The `release()` method reenables invalidation and change notifications.

`close()` is equivalent to `release()`, it is there just to comply with the `AutoCloseable` interface.

`blockWhile(runnable)` is equivalent to
```java
block();
runnable.run();
release();
```

Any number of notifications suppressed while blocked results in a single notification when released.

For convenient use with try-with-resources, `block()` returns `this` (the object on which it was called). Then, to ensure that invalidation and change notifications on `p` are resumed even in case of an exception, you can write
```java
try(AutoCloseable a = p.block()) {
    // stuff that causes multiple invalidations of p
}
```

The implementation is quite straightforward, have a look at, e.g. [SimpleBooleanProperty.java](https://github.com/TomasMikula/InhiBeans/blob/master/src/main/java/inhibeans/property/SimpleBooleanProperty.java).


### Bindings factory methods ###

Inhibitory versions of bindings provide factory methods that wrap an _eager_ `ObservableValue` and return an inhibitory binding. For example:
```java
IntegerProperty a;
IntegerProperty b;
NumberBinding sum = a.add(b);
IntegerBinding relaxedSum = inhibeans.binding.IntegerBinding.wrap(sum);
```
Now, in the following code, change listeners on `sum` fire twice, while change listeners on `relaxedSum` fire only once.
```java
relaxedSum.blockWhile(() -> {
    a.set(1);
    b.set(2);
});
```


Motivational Example
--------------------

Let's implement a logical AND gate. An AND gate has two inputs, `a` and `b`, and one output where the result of `a & b` is pushed. We want all three values to be _observable_ (in the sense of `javafx.beans.value.ObservableValue`). Finally, we need a method to set the inputs.

```java

interface AndGate {
    ObservableBooleanValue a();
    ObservableBooleanValue b();
    ObservableBooleanValue output();
    void setInputs(boolean a, boolean b);
}
```

### Task ###

Provide an implementation with the following properties:
  1. _Correctness_: actually implement an AND gate.
  2. _Consistency_: to any observer (within the same thread), the state of observable values has to appear consistent (i.e. `output = a & b`) at all times.
  3. _Efficiency_: for a single call to `setInputs(a, b)`, the observer receives at most one invalidation notification.

Now take a few minutes to think about how one would implement that.


### Test case ###

You can use this method to test your implementation (don't forget to enable assertions):

```java

void test(AndGate gate) {
    class Counter {
        int count = 0;
        public void inc() { count += 1; }
        public int get() { return count; }
    }

    Predicate<AndGate> consistent = g ->
        g.output().get() == (g.a().get() && g.b().get());

    gate.setInputs(false, false);
    assert gate.output().get() == false;

    Counter na = new Counter();
    Counter nb = new Counter();
    Counter no = new Counter();

    gate.a().addListener(observable -> {
        assert consistent.test(gate);
        na.inc();
    });
    gate.b().addListener(observable -> {
        assert consistent.test(gate);
        nb.inc();
    });
    gate.output().addListener(observable -> {
        assert consistent.test(gate);
        no.inc();
    });

    gate.setInputs(true, true);
    assert gate.output().get() == true;

    assert na.get() == 1;
    assert nb.get() == 1;
    assert no.get() == 1;
}
```

### Solution ###

This is the solution using InhiBeans. There are just two lines added (the ones with comments) to the naive (inefficient, in the above sense) implementation.

```java
import inhibeans.binding.BooleanBinding; // Note BooleanBinding imported from inhibeans.

class AndGateImpl {
    private final BooleanProperty a = new SimpleBooleanProperty();
    private final BooleanProperty b = new SimpleBooleanProperty();
    private final BooleanBinding output = BooleanBinding.wrap(a.and(b));

    @Override
    public void setInputs(boolean a, boolean b) {
        output.block(); // suppress notifications temporarily
        this.a.set(a);
        this.b.set(b);
        output.release(); // continue delivering notifications
    }

    @Override public ObservableBooleanValue a() { return a; }
    @Override public ObservableBooleanValue b() { return b; }
    @Override public ObservableBooleanValue output() { return output; }
}
```

There is also [a runnable version](https://github.com/TomasMikula/InhiBeans/blob/master/src/demo/java/inhibeans/demo/AndGateDemo.java).


Download
--------

Download the JAR file from [here](https://googledrive.com/host/0B4a5AnNnZhkbX0d4QUZXenRUaVE/).


License
-------

[BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
