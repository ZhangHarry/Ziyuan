/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.preferences.component;

import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import tzuyu.plugin.TzuyuPlugin;
import tzuyu.plugin.core.constants.Messages;
import tzuyu.plugin.ui.SWTFactory;

/**
 * @author LLT
 *
 */
public abstract class TableViewerEditablePanel<T> {
	protected static Messages msg = TzuyuPlugin.getMessages();
	public static final int ADD_BTN = 0;
	public static final int EDIT_BTN = 1;
	public static final int REMOVE_BTN = 2;
	protected Composite panel;
	protected TableViewer tableViewer;
	private Composite btnGroup;
	private Button addBtn;
	private Button editBtn;
	private Button removeBtn; 
	
	public TableViewerEditablePanel(Composite parent) {
		/* table on the left */
		panel = createContentPanel(parent);
		GridData data;
		tableViewer = createTableViewer(panel);
		
		/* butons on the right */
		btnGroup = new Composite(panel, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		btnGroup.setLayout(layout);
		data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		btnGroup.setLayoutData(data);
		addBtn = SWTFactory.createBtnAlignFill(btnGroup, msg.common_addButton());
		editBtn = SWTFactory.createBtnAlignFill(btnGroup, msg.common_editButton());
		removeBtn = SWTFactory.createBtnAlignFill(btnGroup, msg.common_removeButton());
		registerListener();
	}
	
	protected void setToInitState() {
		editBtn.setEnabled(false);
		removeBtn.setEnabled(false);
	}

	protected TableViewer createTableViewer(Composite parent) {
		TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.MULTI
				| SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH);
		tableViewer.getTable().setLayoutData(data);
		return tableViewer;
	}

	protected Composite createContentPanel(Composite parent) {
		return SWTFactory.createGridPanel(parent, 2);
	}
	
	@SuppressWarnings("unchecked")
	private void registerListener() {
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection selection = (StructuredSelection) event
						.getSelection();
				onSelectTableRow(selection);
			}

		});
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onAdd();
			}
		});
		
		editBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				T firstElement = (T)((StructuredSelection)tableViewer
						.getSelection()).getFirstElement();
				if (onEdit(firstElement)) {
					tableViewer.refresh(firstElement);
				}
			}
		});
		
		removeBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<T> elements = ((StructuredSelection) tableViewer
						.getSelection()).toList();
				onRemove(elements);
			}
		});
	}
	
	public TableViewer getTableViewer() {
		return tableViewer;
	}

	protected void onSelectTableRow(StructuredSelection selection) {
		int size = selection.size();
		editBtn.setEnabled(size == 1);
		removeBtn.setEnabled(size > 0);
	}
	
	protected void onRemove(List<T> elements) {
		tableViewer.remove(elements.toArray());
	}

	protected boolean onEdit(T firstElement) {
		return false;
	}

	protected abstract void onAdd();

	/**
	 * @param kind 
	 * 		TableViewerEditablePanel.ADD_BTN
	 * 		TableViewerEditablePanel.EDIT_BTN
	 * 		TableViewerEditablePanel.REMOVE_BTN	
	 */
	public void hide(int kind) {
		Button btn = getButton(kind);
		btn.setVisible(false);
		if (btn != removeBtn) {
			btn.moveBelow(removeBtn);
		}
	}
	
	/**
	 * @param kind 
	 * 		TableViewerEditablePanel.ADD_BTN
	 * 		TableViewerEditablePanel.EDIT_BTN
	 * 		TableViewerEditablePanel.REMOVE_BTN	
	 */
	public Button getButton(int kind) {
		if (kind == ADD_BTN) {
			return addBtn;
		}
		if (kind == REMOVE_BTN) {
			return removeBtn;
		}
		if (kind == EDIT_BTN) {
			return editBtn;
		}
		throw new IllegalArgumentException(
				"Cannot find the button with kind = " + kind);
	}
	
	public Button getAddBtn() {
		return addBtn;
	}
	
	public Button getRemoveBtn() {
		return removeBtn;
	}
	
	public GridData getTableLayoutData() {
		return (GridData)tableViewer.getTable().getLayoutData();
	}
	
	public Composite getWidget() {
		return panel;
	}
	
	protected Shell getShell() {
		return panel.getShell();
	}
}
