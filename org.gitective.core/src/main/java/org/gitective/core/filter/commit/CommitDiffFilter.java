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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Commit diff filter that computes the differences introduced by each commit
 * visited and calls {@link #include(RevCommit, Collection)}.
 */
public class CommitDiffFilter extends CommitFilter {

	private static class LocalDiffEntry extends DiffEntry {

		public LocalDiffEntry(final String path) {
			setOldPath(path);
			setNewPath(path);
		}

		private LocalDiffEntry setOldPath(final String path) {
			oldPath = path;
			return this;
		}

		private LocalDiffEntry setNewPath(final String path) {
			newPath = path;
			return this;
		}

		private LocalDiffEntry setNewMode(final FileMode mode) {
			newMode = mode;
			return this;
		}

		private LocalDiffEntry setChangeType(final ChangeType type) {
			changeType = type;
			return this;
		}

		private LocalDiffEntry setNewId(final AbbreviatedObjectId id) {
			newId = id;
			return this;
		}
	}

	private static RevTree getTree(final RevWalk walk, final RevCommit commit)
			throws IOException {
		RevTree tree = commit.getTree();
		if (tree == null)
			tree = walk.parseCommit(commit).getTree();
		return tree;
	}

	@Override
	public boolean include(final RevWalk walker, final RevCommit commit)
			throws IOException {
		final List<DiffEntry> diffs;

		final TreeWalk walk = new TreeWalk(walker.getObjectReader());
		walk.setRecursive(true);
		walk.setFilter(TreeFilter.ANY_DIFF);

		switch (commit.getParentCount()) {
		case 0:
			walk.addTree(new EmptyTreeIterator());
			walk.addTree(commit.getTree());
			diffs = DiffEntry.scan(walk);
			break;
		case 1:
			walk.addTree(getTree(walker, commit.getParent(0)));
			walk.addTree(commit.getTree());
			diffs = DiffEntry.scan(walk);
			break;
		default:
			final RevCommit[] parents = commit.getParents();
			final int parentCount = parents.length;

			// Add all parent trees with current commit tree last
			for (int i = 0; i < parentCount; i++)
				walk.addTree(getTree(walker, parents[i]));
			walk.addTree(commit.getTree());

			diffs = new ArrayList<DiffEntry>();
			final MutableObjectId objectId = new MutableObjectId();
			while (walk.next()) {
				final int currentMode = walk.getRawMode(parentCount);
				int parentMode = 0;
				boolean same = false;
				for (int i = 0; i < parentCount; i++) {
					final int mode = walk.getRawMode(i);
					same = mode == currentMode && walk.idEqual(parentCount, i);
					if (same)
						break;
					parentMode |= mode;
				}
				if (same)
					continue;

				final LocalDiffEntry diff = new LocalDiffEntry(
						walk.getPathString());
				diff.setNewMode(FileMode.fromBits(currentMode));
				walk.getObjectId(objectId, parentCount);
				diff.setNewId(AbbreviatedObjectId.fromObjectId(objectId));
				if (parentMode == 0 && currentMode != 0)
					diff.setChangeType(ChangeType.ADD);
				else if (parentMode != 0 && currentMode == 0)
					diff.setChangeType(ChangeType.DELETE);
				else
					diff.setChangeType(ChangeType.MODIFY);
				diffs.add(diff);
			}
		}

		return include(include(commit, diffs));
	}

	/**
	 * Handle the differences introduced by given commit.
	 * 
	 * Sub-classes should override this method.
	 * 
	 * @param commit
	 * @param diffs
	 * @return true to continue, false to abort
	 */
	protected boolean include(final RevCommit commit,
			final Collection<DiffEntry> diffs) {
		return true;
	}

	/**
	 * Returns this filter.
	 * 
	 * Sub-classes should override to return an actual cloned filter.
	 */
	@Override
	public RevFilter clone() {
		return this;
	}
}
