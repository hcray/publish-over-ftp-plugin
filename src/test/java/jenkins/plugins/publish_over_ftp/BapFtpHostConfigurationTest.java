/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over_ftp;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.util.SecretHelper;
import jenkins.plugins.publish_over.BPBuildInfo;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.expect;

public class BapFtpHostConfigurationTest {

    @BeforeClass
    public static void before() {
        SecretHelper.setSecretKey();
    }

    @AfterClass
    public static void after() {
        SecretHelper.clearSecretKey();
    }

    private BPBuildInfo buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), null, null);
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private FTPClient mockFTPClient = mockControl.createMock(FTPClient.class);
    private BapFtpHostConfiguration bapFtpHostConfiguration = new BapFtpHostConfigurationWithMockFTPClient(mockFTPClient);

    @Test public void testChangeToRootDir() throws Exception {
        testChangeToInitialDirectory("/");
    }

    @Test public void testChangeToRootDirWin() throws Exception {
        testChangeToInitialDirectory("\\");
    }

    @Test public void testChangeToRootDirLongerPath() throws Exception {
        testChangeToInitialDirectory("/this/is/my/root");
    }

    @Test public void testChangeToRootDirRelativePath() throws Exception {
        testChangeToInitialDirectory("this/is/my/rel/root", true);
    }

    @Test public void testNoChangeDirectoryRemoteDirNull() throws Exception {
        testNoChangeToInitialDirectory(null);
    }

    @Test public void testNoChangeDirectoryRemoteDirEmptyString() throws Exception {
        testNoChangeToInitialDirectory("");
    }

    @Test public void testNoChangeDirectoryRemoteDirOnlySpaceInString() throws Exception {
        testNoChangeToInitialDirectory("  ");
    }

    private void testNoChangeToInitialDirectory(final String remoteRoot) throws Exception {
        bapFtpHostConfiguration.setRemoteRootDir(remoteRoot);
        expectConnectAndLogin();
        expect(mockFTPClient.printWorkingDirectory()).andReturn("/pub");

        BapFtpClient client = assertCreateSession();
        assertEquals("/pub", client.getAbsoluteRemoteRoot());
    }

    private void testChangeToInitialDirectory(final String remoteRoot) throws Exception {
        testChangeToInitialDirectory(remoteRoot, false);
    }

    private void testChangeToInitialDirectory(final String remoteRoot, final boolean expectPwd) throws Exception {
        bapFtpHostConfiguration.setRemoteRootDir(remoteRoot);
        expectConnectAndLogin();
        expect(mockFTPClient.changeWorkingDirectory(remoteRoot)).andReturn(true);
        if (expectPwd)
            expect(mockFTPClient.printWorkingDirectory()).andReturn("/" + remoteRoot);
        BapFtpClient client = assertCreateSession();
        if (!expectPwd)
            assertEquals(remoteRoot, client.getAbsoluteRemoteRoot());
    }

    @Test public void testSetActive() throws Exception {
        bapFtpHostConfiguration.setUseActiveData(true);
        expectConnectAndLogin();
        expect(mockFTPClient.printWorkingDirectory()).andReturn("/");
        assertCreateSession();
    }

    private void expectConnectAndLogin() throws Exception {
        mockFTPClient.setDefaultTimeout(bapFtpHostConfiguration.getTimeout());
        mockFTPClient.setDataTimeout(bapFtpHostConfiguration.getTimeout());
        mockFTPClient.connect(bapFtpHostConfiguration.getHostname(), bapFtpHostConfiguration.getPort());
        expect(mockFTPClient.getReplyCode()).andReturn(FTPReply.SERVICE_READY);
        if (bapFtpHostConfiguration.isUseActiveData()) {
            mockFTPClient.enterLocalActiveMode();
        } else {
            mockFTPClient.enterLocalPassiveMode();
        }
        expect(mockFTPClient.login(bapFtpHostConfiguration.getUsername(), bapFtpHostConfiguration.getPassword())).andReturn(true);
    }

    private BapFtpClient assertCreateSession() throws IOException {
        mockControl.replay();
        BapFtpClient client = bapFtpHostConfiguration.createClient(buildInfo);
        mockControl.verify();
        return client;
    }

    private static class BapFtpHostConfigurationWithMockFTPClient extends BapFtpHostConfiguration {
        static final long serialVersionUID = 1L;
        private static final String TEST_CFG_NAME = "myTestConfig";
        private static final String TEST_HOSTNAME = "my.test.hostname";
        private static final String TEST_USERNAME = "myTestUsername";
        private static final String TEST_PASSWORD = "myTestPassword";
        private FTPClient ftpClient;
        BapFtpHostConfigurationWithMockFTPClient(final FTPClient ftpClient) {
            super(TEST_CFG_NAME, TEST_HOSTNAME, TEST_USERNAME, TEST_PASSWORD, "", DEFAULT_PORT, DEFAULT_TIMEOUT, false);
            this.ftpClient = ftpClient;
        }
        @Override
        public FTPClient createFTPClient() {
            return ftpClient;
        }
    }

}
