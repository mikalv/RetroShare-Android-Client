package org.retroshare.android;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rsctrl.core.Core;
import rsctrl.core.Core.File;
import rsctrl.core.Core.Status.StatusCode;
import rsctrl.files.Files;
import rsctrl.files.Files.RequestControlDownload;
import rsctrl.files.Files.RequestControlDownload.Action;
import rsctrl.files.Files.RequestTransferList;
import rsctrl.files.Files.ResponseTransferList;

import org.retroshare.android.RsCtrlService.RsMessage;

import com.google.protobuf.InvalidProtocolBufferException;

public class RsFilesService implements RsServiceInterface
{
	RsCtrlService mRsCtrlService;
	UiThreadHandlerInterface mUiThreadHandler;
	
	RsFilesService(RsCtrlService s, UiThreadHandlerInterface u)
	{
		mRsCtrlService = s;
		mUiThreadHandler = u;
	}
	
	public static interface FilesServiceListener
	{
		public void update();
	}
	
	private Set<FilesServiceListener>mListeners = new HashSet<FilesServiceListener>();
	public void registerListener(FilesServiceListener l) { mListeners.add(l); }
	public void unregisterListener(FilesServiceListener l) { mListeners.remove(l); }
	private void _notifyListeners() { if(mUiThreadHandler != null) mUiThreadHandler.postToUiThread(new Runnable(){public void run(){for(FilesServiceListener l:mListeners) l.update();}}); }
	
	List<Files.FileTransfer> transfersUp   = new ArrayList<Files.FileTransfer>();
	public List<Files.FileTransfer> getTransfersUp() { return transfersUp; }
	
	List<Files.FileTransfer> transfersDown = new ArrayList<Files.FileTransfer>();
	public List<Files.FileTransfer> getTransfersDown() { return transfersDown; }
	
	public void updateTransfers(Files.Direction d)
	{
		RequestTransferList.Builder reqb = RequestTransferList.newBuilder();
    	reqb.setDirection(d);
    	RsMessage msg=new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.FILES_VALUE<<8)|Files.RequestMsgIds.MsgId_RequestTransferList_VALUE;
    	msg.body=reqb.build().toByteArray();
    	mRsCtrlService.sendMsg(msg,new ResponseTransferListHandler(d));
	}
	
	private class ResponseTransferListHandler extends RsMessageHandler
	{
		Files.Direction mDirection;
		
		ResponseTransferListHandler(Files.Direction d)
		{
			super();
			mDirection = d;
		}

		@Override
		protected void rsHandleMsg(RsMessage msg)
		{
			try
			{
				ResponseTransferList resp=ResponseTransferList.parseFrom(msg.body);
				if(resp.getStatus().getCode().equals(StatusCode.SUCCESS))
				{
					List<Files.FileTransfer> files=ResponseTransferList.parseFrom(msg.body).getTransfersList();
					if(mDirection.equals(Files.Direction.DIRECTION_UPLOAD)) transfersUp=files;
					else transfersDown = files;
					_notifyListeners();
				}
			}
			catch (InvalidProtocolBufferException e) { e.printStackTrace(); }
		}
	}
	
	@Override
	public void handleMessage(RsMessage m) {}
	
	public void sendRequestControlDownload(File file, Action action,RsMessageHandler handler)
	{
    	RsMessage msg = new RsMessage();
    	msg.msgId = (Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.FILES_VALUE<<8)|Files.RequestMsgIds.MsgId_RequestControlDownload_VALUE;
    	msg.body = RequestControlDownload.newBuilder().setFile(file).setAction(action).build().toByteArray();
    	mRsCtrlService.sendMsg(msg,handler);
	}
	public void sendRequestControlDownload(File file, Action action) { sendRequestControlDownload(file, action, null); }
	
}
