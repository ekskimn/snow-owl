/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.datastore.index;

import java.io.Reader;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;

import bak.pcj.set.IntOpenHashSet;
import bak.pcj.set.IntSet;
import bak.pcj.set.UnmodifiableIntSet;

import com.b2international.snowowl.core.TextConstants;

/**
 * A character-oriented tokenizer which splits tokens on whitespace and delimiters enumerated in
 * {@link IndexUtils#DELIMITERS}, and also converts characters to lower case in the normalization phase.
 * 
 */
public class DelimiterTokenizer extends CharTokenizer {

	// Excludes 2000-2000a, which is handled as a range
	private static final String BREAKING_WHITESPACE_CHARS = "\t\n\013\f\r \u0085\u1680\u2028\u2029\u205f\u3000";

	// Excludes 2007, which is handled as a gap in a pair of ranges
	private static final String NON_BREAKING_WHITESPACE_CHARS = "\u00a0\u180e\u202f";
	
	/**
	 * @see TextConstants#WHITESPACE_OR_DELIMITER_MATCHER
	 */
	private static final IntSet TOKEN_CHARS;
	
	static {
		
		final IntSet set = new IntOpenHashSet();
		
		char[] charArray = TextConstants.DELIMITERS.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			set.add(charArray[i]);
		}
		
		charArray = BREAKING_WHITESPACE_CHARS.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			set.add(charArray[i]);
		}
		
		charArray = NON_BREAKING_WHITESPACE_CHARS.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			set.add(charArray[i]);
		}
		
		final int first = '\u2000';
		final int last = '\u200a';
		
		for (int i = first; i < last; i++) {
			set.add(i);
		}
		
		set.trimToSize();
		
		TOKEN_CHARS = new UnmodifiableIntSet(set);
		
	}
	
	public DelimiterTokenizer(Reader input) {
		super(Version.LUCENE_4_9, input);
	}
	
	@Override
	protected int normalize(int c) {
		return Character.toLowerCase(c);
	}
	
	@Override
	protected boolean isTokenChar(int c) {
		// We don't have whitespace characters to match in the supplementary code point range
		return c >= Character.MIN_SUPPLEMENTARY_CODE_POINT || !TOKEN_CHARS.contains(c);
	}
}
