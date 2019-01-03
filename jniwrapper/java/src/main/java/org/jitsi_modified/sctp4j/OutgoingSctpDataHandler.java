package org.jitsi_modified.sctp4j;

@FunctionalInterface
interface FourArgumentIntFunc<One, Two, Three, Four> {
    int apply(One one, Two two, Three three, Four four);
}

/**
 * Handler for packets which the SCTP stack wants to send out to the network
 */
public interface OutgoingSctpDataHandler extends FourArgumentIntFunc<Long, byte[], Integer, Integer> {
    @Override
    int apply(Long aLong, byte[] bytes, Integer integer, Integer integer2);
}
