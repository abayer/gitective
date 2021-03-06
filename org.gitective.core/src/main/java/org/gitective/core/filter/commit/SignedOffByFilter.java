/*
 * Copyright (c) 2011 Kevin Sawicki <kevinsawicki@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package org.gitective.core.filter.commit;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.filter.RevFilter;

/**
 * Commit message pattern filter that includes commit that have a Signed-off-by
 * line for a person.
 */
public class SignedOffByFilter extends CommitMessageFindFilter {

	/**
	 * Signed off by format
	 */
	public static final String SIGNED_OFF_BY = Constants.SIGNED_OFF_BY_TAG
			+ "{0} <{1}>";

	private final PersonIdent person;

	/**
	 * Create a signed off by filter matching the person
	 * 
	 * @param person
	 *            must be non-null
	 */
	public SignedOffByFilter(final PersonIdent person) {
		super(Pattern.quote(MessageFormat.format(SIGNED_OFF_BY,
				person.getName(), person.getEmailAddress())), Pattern.MULTILINE);
		this.person = person;
	}

	@Override
	public RevFilter clone() {
		return new SignedOffByFilter(person);
	}
}
