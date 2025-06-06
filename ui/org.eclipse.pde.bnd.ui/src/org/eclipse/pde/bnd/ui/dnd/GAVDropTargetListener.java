/*******************************************************************************
 * Copyright (c) 2020 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.dnd;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundle;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundleVersion;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryResourceElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.osgi.resource.Resource;

import aQute.bnd.service.RepositoryPlugin;

public abstract class GAVDropTargetListener implements DropTargetListener {

	public class FormatEvent {
		private final Resource			resource;
		private final RepositoryPlugin	repositoryPlugin;
		private final boolean			noVersion;
		private final boolean			useAlternateSyntax;

		public FormatEvent(Resource resource, RepositoryPlugin repositoryPlugin, boolean noVersion,
			boolean useAlternateSyntax) {
			this.resource = resource;
			this.repositoryPlugin = repositoryPlugin;
			this.noVersion = noVersion;
			this.useAlternateSyntax = useAlternateSyntax;
		}

		public Resource getResource() {
			return resource;
		}

		public RepositoryPlugin getRepositoryPlugin() {
			return repositoryPlugin;
		}

		public boolean isNoVersion() {
			return noVersion;
		}

		public boolean useAlternateSyntax() {
			return useAlternateSyntax;
		}

		public String getLineAtInsertionPoint() {
			return styledText.getLine(styledText.getLineAtOffset(styledText.getCaretOffset()));
		}

		public String getIndentPrefix() {
			return styledText.getLine(styledText.getLineAtOffset(styledText.getCaretOffset()))
				.split("\\S", 2)[0];
		}

	}

	private volatile boolean				alternateSyntaxEnabled	= false;

	private final TextTransfer				textTransfer			= TextTransfer.getInstance();
	private final LocalSelectionTransfer	localSelectionTransfer	= LocalSelectionTransfer.getTransfer();
	private final StyledText				styledText;

	public GAVDropTargetListener(StyledText styledText) {
		this.styledText = requireNonNull(styledText);
		addAlternateSyntaxKeyListener(this.styledText);
	}

	abstract void format(FormatEvent formatEvent);

	public StyledText getStyledText() {
		return styledText;
	}

	abstract boolean hasAlternateSyntax();

	public final boolean isAlternateSyntaxEnabled() {
		return alternateSyntaxEnabled;
	}

	@Override
	public void drop(DropTargetEvent event) {
		if (textTransfer.isSupportedType(event.currentDataType)) {
			// always move the caret to the end of the line
			styledText.invokeAction(ST.LINE_END);

			ISelection selection = localSelectionTransfer.getSelection();
			Iterator<?> iterator = ((IStructuredSelection) selection).iterator();
			while (iterator.hasNext()) {
				Object item = iterator.next();
				if (item instanceof RepositoryBundle rb) {
					format(new FormatEvent(rb.getResource(), rb.getRepo(), true, isAlternateSyntaxEnabled()));
					break;
				} else if (item instanceof RepositoryBundleVersion rbv) {
					format(new FormatEvent(rbv.getResource(), rbv.getRepo(), false, isAlternateSyntaxEnabled()));
					break;
				} else if (item instanceof RepositoryResourceElement rbe) {
					RepositoryBundleVersion rbv = rbe.getRepositoryBundleVersion();
					format(new FormatEvent(rbv.getResource(), rbv.getRepo(), true, isAlternateSyntaxEnabled()));
					break;
				}
			}
		}
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT) {
			if ((event.operations & DND.DROP_COPY) != 0) {
				event.detail = DND.DROP_COPY;
			} else {
				event.detail = DND.DROP_NONE;
			}
		}
	}

	@Override
	public void dragLeave(DropTargetEvent event) {}

	@Override
	public void dragOperationChanged(DropTargetEvent event) {}

	@Override
	public void dragOver(DropTargetEvent event) {}

	@Override
	public void dropAccept(DropTargetEvent event) {}

	public static String indent(boolean tabs, int size) {
		return new String(new char[size]).replace("\0", tabs ? "\t" : " ");
	}

	private void addAlternateSyntaxKeyListener(Control control) {
		if (!hasAlternateSyntax()) {
			return;
		}
		Display display = control.getDisplay();
		display.addFilter(SWT.KeyDown, event -> {
			if (event.keyCode == SWT.ALT) {
				alternateSyntaxEnabled = true;
			}
		});
		display.addFilter(SWT.KeyUp, event -> {
			if (event.keyCode == SWT.ALT) {
				alternateSyntaxEnabled = false;
			}
		});
	}

}
