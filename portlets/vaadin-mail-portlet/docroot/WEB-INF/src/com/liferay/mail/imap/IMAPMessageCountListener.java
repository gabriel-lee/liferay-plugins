/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.mail.imap;

import com.liferay.mail.MailException;
import com.liferay.mail.model.Account;
import com.liferay.mail.model.Folder;
import com.liferay.mail.service.FolderLocalServiceUtil;
import com.liferay.mail.service.MessageLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.User;

import com.sun.mail.imap.IMAPFolder;

import javax.mail.Message;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

/**
 * @author Scott Lee
 */
public class IMAPMessageCountListener implements MessageCountListener {

	public IMAPMessageCountListener(
		User user, Account account, String password) {

		_account = account;
		_imapAccessor = new IMAPAccessor(user, account, password);
	}

	public void messagesAdded(MessageCountEvent messageCountEvent) {
		Message[] jxMessages = messageCountEvent.getMessages();

		IMAPFolder imapFolder = null;

		try {
			imapFolder = _imapAccessor.openFolder(
				(IMAPFolder)jxMessages[0].getFolder());

			Folder folder = FolderLocalServiceUtil.getFolder(
				_account.getAccountId(), imapFolder.getFullName());

			_imapAccessor.storeEnvelopes(
				folder.getFolderId(), imapFolder, jxMessages);
		}
		catch (Exception e) {
			_log.error("Unable to add messages", e);
		}
		finally {
			try {
				_imapAccessor.closeFolder(imapFolder, false);
			}
			catch (MailException me) {
				_log.error(me, me);
			}
		}
	}

	public void messagesRemoved(MessageCountEvent messageCountEvent) {
		Message[] jxMessages = messageCountEvent.getMessages();

		IMAPFolder imapFolder = null;

		try {
			imapFolder = _imapAccessor.openFolder(
				(IMAPFolder)jxMessages[0].getFolder());

			Folder folder = FolderLocalServiceUtil.getFolder(
				_account.getAccountId(), imapFolder.getFullName());

			long[] remoteMessageIds = _imapAccessor.getMessageUIDs(
				imapFolder, jxMessages);

			for (long remoteMessageId : remoteMessageIds) {
				com.liferay.mail.model.Message message =
					MessageLocalServiceUtil.getMessage(
						folder.getFolderId(), remoteMessageId);

				MessageLocalServiceUtil.deleteMessage(message.getMessageId());
			}
		}
		catch (Exception e) {
			_log.error("Unable to delete messages", e);
		}
		finally {
			try {
				_imapAccessor.closeFolder(imapFolder, false);
			}
			catch (MailException me) {
				_log.error(me);
			}
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		IMAPMessageCountListener.class);

	private Account _account;
	private IMAPAccessor _imapAccessor;

}