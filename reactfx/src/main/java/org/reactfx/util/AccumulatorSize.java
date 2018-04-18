package org.reactfx.util;

/**
 * In the context of a singly-linked list, determines whether there is an empty list of value ({@link #ZERO}),
 * a list with a single value ({@link #ONE}) that can be gotten via {@code list.head()}
 * or whether there are multiple values ({@link #MANY}) that can be gotten via {@code list.tail()}
 */
public enum AccumulatorSize {
    ZERO, ONE, MANY;

    public static AccumulatorSize fromInt(int n) {
        if(n < 0) {
            throw new IllegalArgumentException("Size cannot be negative: " + n);
        } else switch(n) {
            case 0: return ZERO;
            case 1: return ONE;
            default: return MANY;
        }
    }
}