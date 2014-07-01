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
package com.codenvy.eclipse.ui.widgets;

import static com.codenvy.eclipse.core.utils.StringHelper.isNullOrEmpty;
import static org.eclipse.jface.dialogs.IDialogConstants.OK_ID;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog used by the user to provide it's credentials.
 * 
 * @author Kevin Pollet
 */
public class CredentialsDialog extends TitleAreaDialog {
    private Text    passwordText;
    private Button  storeUserCredentialsButton;
    private String  username;
    private String  password;
    private boolean storeUserCredentials;

    /**
     * Constructs an instance of {@link CredentialsDialog}.
     * 
     * @param username the username.
     * @param parentShell the parent {@link Shell}.
     */
    public CredentialsDialog(String username, Shell parentShell) {
        super(parentShell);

        this.username = username;
    }

    @Override
    public void create() {
        super.create();

        setTitle("Authentication");
        setMessage("Authenticate with the Codenvy platform");

        getButton(OK_ID).setEnabled(false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite contents = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
        contents.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        contents.setLayout(new GridLayout(2, false));

        final Label usernameLabel = new Label(contents, SWT.NONE);
        usernameLabel.setText("Username:");

        final Text usernameText = new Text(contents, SWT.BORDER | SWT.READ_ONLY);
        usernameText.setText(username);
        usernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        final Label passwordLabel = new Label(contents, SWT.NONE);
        passwordLabel.setText("Password:");

        passwordText = new Text(contents, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        passwordText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                getButton(OK_ID).setEnabled(!isNullOrEmpty(passwordText.getText()));
            }
        });

        storeUserCredentialsButton = new Button(contents, SWT.CHECK);
        storeUserCredentialsButton.setText("Store these user credentials in Eclipse secure storage.");
        storeUserCredentialsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        return contents;
    }

    @Override
    protected void okPressed() {
        this.password = passwordText.getText();
        this.storeUserCredentials = storeUserCredentialsButton.getSelection();
        super.okPressed();
    }

    /**
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return
     */
    public boolean isStoreUserCredentials() {
        return storeUserCredentials;
    }

    /**
     * @return
     */
    public String getUsername() {
        return username;
    }
}