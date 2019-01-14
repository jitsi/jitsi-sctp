/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

