/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * A subclass of {@link IndexWriter} that exposes its internal counters.
 * 
 * @since 4.6
 */
public class CustomIndexWriter extends IndexWriter {

	public CustomIndexWriter(Directory d, IndexWriterConfig conf) throws IOException {
		super(d, conf);
	}
	
	public long getLastGeneration() {
		return segmentInfos.getLastGeneration();
	}
	
	public int getSegmentCounter() {
		return segmentInfos.counter;
	}
}
