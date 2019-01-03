package org.jitsi_modified.sctp4j;


@FunctionalInterface
interface EightArgumentVoidFunc<One, Two, Three, Four, Five, Six, Seven, Eight> {
    void apply(One one, Two two, Three three, Four four, Five five, Six six, Seven seven, Eight eight);
}

/**
 * Handler for packets which have been received from the network, passed through the SCTP stack and are ready for
 * processing by the application
 */
public interface IncomingSctpDataHandler extends EightArgumentVoidFunc<Long, byte[], Integer, Integer, Integer, Long, Integer, Integer> {
    @Override
    void apply(Long aLong, byte[] bytes, Integer integer, Integer integer2, Integer integer3, Long aLong2, Integer integer4, Integer integer5);
}

