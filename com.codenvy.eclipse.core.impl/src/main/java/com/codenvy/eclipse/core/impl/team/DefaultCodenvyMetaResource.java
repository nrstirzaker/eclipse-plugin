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
package com.codenvy.eclipse.core.impl.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.RepositoryProvider;

import com.codenvy.eclipse.core.impl.DefaultProjectService;
import com.codenvy.eclipse.core.model.CodenvyProject;
import com.codenvy.eclipse.core.model.CodenvyToken;
import com.codenvy.eclipse.core.services.ProjectService;
import com.codenvy.eclipse.core.team.CodenvyMetaProject;
import com.codenvy.eclipse.core.team.CodenvyMetaResource;
import com.codenvy.eclipse.core.team.CodenvyProvider;

/**
 * The default Codenvy resource mapping class implementation.
 * 
 * @author Kevin Pollet
 */
public class DefaultCodenvyMetaResource implements CodenvyMetaResource {
    private final IResource resource;
    private final boolean   tracked;

    public DefaultCodenvyMetaResource(IResource resource) {
        this.resource = resource;

        final CodenvyProvider codenvyProvider = (CodenvyProvider)RepositoryProvider.getProvider(resource.getProject(), CodenvyProvider.PROVIDER_ID);
        if (codenvyProvider != null) {
            final CodenvyMetaProject metaProject = codenvyProvider.getMetaProject();

            if (metaProject != null) {
                final ProjectService projectService = new DefaultProjectService(metaProject.url, new CodenvyToken(metaProject.codenvyToken));
                final CodenvyProject codenvyProject = new CodenvyProject.Builder().withName(metaProject.projectName)
                                                                                  .withWorkspaceId(metaProject.workspaceId)
                                                                                  .build();

                this.tracked = projectService.isResourceInProject(codenvyProject, resource.getProjectRelativePath().toString());
            }
            else {
                this.tracked = false;
            }
        }
        else {
            this.tracked = false;
        }
    }

    public IResource getResource() {
        return resource;
    }

    public boolean isTracked() {
        return tracked;
    }
}
