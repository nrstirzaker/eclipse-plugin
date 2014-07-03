/*
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.eclipse.ui.wizard.exporter.pages;

import static org.eclipse.jface.viewers.CheckboxTableViewer.newCheckList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.codenvy.eclipse.ui.CodenvyUIPlugin;
import com.codenvy.eclipse.ui.Images;
import com.codenvy.eclipse.ui.wizard.importer.pages.ProjectWizardPage;

/**
 * @author Stéphane Daviet
 */
public class ExportCodenvyProjectsPage extends WizardPage implements IPageChangedListener {
    private CheckboxTableViewer projectsTableViewer;
    private List<IProject>      selectedProjects;

    public ExportCodenvyProjectsPage() {
        super(ProjectWizardPage.class.getSimpleName());

        setTitle("Select workspaces project");
        setDescription("Select local projects that you want to push to a remote Codenvy repository.");
        setImageDescriptor(CodenvyUIPlugin.getDefault().getImageRegistry().getDescriptor(Images.WIZARD_LOGO));
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        final Composite wizardContainer = new Composite(parent, SWT.NONE);
        wizardContainer.setLayout(new GridLayout(2, false));
        wizardContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Label workspaceTableLabel = new Label(wizardContainer, SWT.NONE);
        workspaceTableLabel.setText("Remote Codenvy Workspaces:");
        workspaceTableLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        projectsTableViewer = newCheckList(wizardContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        projectsTableViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return element instanceof IProject ? ((IProject)element).getName() : super.getText(element);
            }
        });
        projectsTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        projectsTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        projectsTableViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                if (event.getChecked()) {
                    selectedProjects.add((IProject)event.getElement());
                } else {
                    selectedProjects.remove(event.getElement());
                }
                setPageComplete(projectsTableViewer.getCheckedElements().length > 0);
            }
        });

        final Composite projectTableButtonsContainer = new Composite(wizardContainer, SWT.NONE);
        projectTableButtonsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));
        projectTableButtonsContainer.setLayout(new GridLayout());

        final Button selectAll = new Button(projectTableButtonsContainer, SWT.NONE);
        selectAll.setText("Select All");
        selectAll.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        selectAll.addSelectionListener(new SelectionAdapter() {
            @SuppressWarnings("unchecked")
            @Override
            public void widgetSelected(SelectionEvent e) {
                projectsTableViewer.setAllChecked(true);
                selectedProjects.clear();
                selectedProjects.addAll((Collection< ? extends IProject>)Arrays.asList(projectsTableViewer.getCheckedElements()));
                setPageComplete(projectsTableViewer.getCheckedElements().length > 0);
            }
        });

        final Button deselectAll = new Button(projectTableButtonsContainer, SWT.NONE);
        deselectAll.setText("Deselect All");
        deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        deselectAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                projectsTableViewer.setAllChecked(false);
                selectedProjects.clear();
                setPageComplete(projectsTableViewer.getCheckedElements().length > 0);
            }
        });

        initializeProjects();

        setControl(wizardContainer);
    }

    @Override
    public void pageChanged(PageChangedEvent event) {
        if (isCurrentPage()) {
            projectsTableViewer.setInput(null);
            initializeProjects();
            setPageComplete(projectsTableViewer.getCheckedElements().length > 0);
        }
    }

    private void initializeProjects() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        projectsTableViewer.setInput(projects);
        // Check any necessary projects
        if (selectedProjects != null) {
            projectsTableViewer.setCheckedElements(selectedProjects.toArray(new IProject[selectedProjects.size()]));
        }
    }

    public void setSelectedProjects(List<IProject> selectedProjects) {
        this.selectedProjects = new ArrayList<IProject>(selectedProjects);
    }

    public List<IProject> getSelectedProjects() {
        return selectedProjects;
    }
}