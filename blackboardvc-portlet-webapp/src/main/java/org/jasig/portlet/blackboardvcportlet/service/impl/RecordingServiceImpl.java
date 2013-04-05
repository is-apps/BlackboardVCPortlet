package org.jasig.portlet.blackboardvcportlet.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.portlet.PortletPreferences;

import org.jasig.portlet.blackboardvcportlet.dao.BlackboardSessionDao;
import org.jasig.portlet.blackboardvcportlet.dao.BlackboardUserDao;
import org.jasig.portlet.blackboardvcportlet.dao.SessionRecordingDao;
import org.jasig.portlet.blackboardvcportlet.data.BlackboardSession;
import org.jasig.portlet.blackboardvcportlet.data.BlackboardUser;
import org.jasig.portlet.blackboardvcportlet.data.SessionRecording;
import org.jasig.portlet.blackboardvcportlet.service.RecordingService;
import org.jasig.portlet.blackboardvcportlet.service.SessionService;
import org.jasig.portlet.blackboardvcportlet.service.util.SASWebServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elluminate.sas.ListRecordingLong;
import com.elluminate.sas.ListRecordingLongResponseCollection;
import com.elluminate.sas.RecordingLongResponse;
import com.elluminate.sas.RemoveRecording;
import com.elluminate.sas.SuccessResponse;

@Service("recordingService")
public class RecordingServiceImpl implements RecordingService {
    private static final Logger logger = LoggerFactory.getLogger(RecordingService.class);

	private SASWebServiceTemplate sasWebServiceTemplate;
	private BlackboardUserDao blackboardUserDao;
	private BlackboardSessionDao blackboardSessionDao;
	private SessionRecordingDao sessionRecordingDao;
    private SessionService sessionService;
    
    
    @Autowired
	public void setBlackboardUserDao(BlackboardUserDao blackboardUserDao) {
        this.blackboardUserDao = blackboardUserDao;
    }

    @Autowired
    public void setBlackboardSessionDao(BlackboardSessionDao blackboardSessionDao) {
        this.blackboardSessionDao = blackboardSessionDao;
    }

    @Autowired
    public void setSessionRecordingDao(SessionRecordingDao sessionRecordingDao) {
        this.sessionRecordingDao = sessionRecordingDao;
    }

    @Autowired
	public void setSasWebServiceTemplate(SASWebServiceTemplate sasWebServiceTemplate)
	{
		this.sasWebServiceTemplate = sasWebServiceTemplate;
	}

    @Autowired
	public void setSessionService(SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	/**
     * Get the recordings for a session
     * @param sessionId Long
     * @return Set<RecordingShort>
     */
    public Set<SessionRecording> getRecordingsForSession(long sessionId)
    {
        return blackboardSessionDao.getSessionRecordings(sessionId);
    }
    
    /**
     * Get a specific recording
     * @param recordingId Long
     * @return RecordingShort
     */
    public SessionRecording getRecording(long recordingId)
    {
        return sessionRecordingDao.getSessionRecording(recordingId);
    }
    
    /**
     * Get the recordings for a user
     * @param uid String
     * @return Set<RecordingShort>
     */
    public Set<SessionRecording> getRecordingsForUser(String uid)
    {
        //TODO this is not bad if the data is all in cache but if it isn't it would be better to just run a query
        
        final BlackboardUser blackboardUser = this.blackboardUserDao.getBlackboardUser(uid);
        
        final Set<SessionRecording> recordings = new LinkedHashSet<SessionRecording>();
        
        final Set<BlackboardSession> chairedSessionsForUser = this.blackboardUserDao.getChairedSessionsForUser(blackboardUser.getUserId());
        for (final BlackboardSession blackboardSession : chairedSessionsForUser) {
            final Set<SessionRecording> sessionRecordings = this.blackboardSessionDao.getSessionRecordings(blackboardSession.getSessionId());
            recordings.addAll(sessionRecordings);
        }
        
        final Set<BlackboardSession> nonChairedSessionsForUser = this.blackboardUserDao.getNonChairedSessionsForUser(blackboardUser.getUserId());
        for (final BlackboardSession blackboardSession : nonChairedSessionsForUser) {
            final Set<SessionRecording> sessionRecordings = this.blackboardSessionDao.getSessionRecordings(blackboardSession.getSessionId());
            recordings.addAll(sessionRecordings);
        }

        return recordings;
    }
    
    /**
     * Get recordings as Admin
     * @return Set<RecordingShort>
     */
    public Set<SessionRecording> getRecordingsForAdmin()
    {
        return sessionRecordingDao.getAllRecordings();
    }
   
    /**
     * Delete a recording
     * @param prefs PortletPreferences
     * @param recordingId Long
     * @throws Exception 
     */
    public void deleteRecording(PortletPreferences prefs, long recordingId) throws Exception
    {
        logger.debug("deleteRecording called");
        
        try
        {
			RemoveRecording removeRecording = new RemoveRecording();
			removeRecording.setRecordingId(recordingId);
			SuccessResponse successResponse = (SuccessResponse) sasWebServiceTemplate.marshalSendAndReceiveToSAS("http://sas.elluminate.com/RemoveRecording", removeRecording);
			logger.debug("removeRecording response:" + successResponse);
		}
        catch (Exception e)
        {
            logger.error("Exception caught calling Collaborate API",e);
            throw e;
        }
        logger.debug("Deleted recordingUrl");
             
        //TODO probably shouldn't delete it from our local db if the WS call fails
        sessionRecordingDao.deleteRecordings(recordingId);
        logger.debug("Deleted recordingShort");
    }
   
    /**
     * Updates the local recordings cache from Collaborate for a particular session
     *
	 * @param sessionId Long
	 * @return Set<RecordingShort>
     */
    public Set<SessionRecording> updateSessionRecordings(long sessionId)
    {
        final BlackboardSession session = sessionService.getSession(sessionId);
        if (session == null) {
            //TODO?
        }
        
        Set<SessionRecording> recordingList = new LinkedHashSet<SessionRecording>();
        try
        {
            ListRecordingLong listRecording = new ListRecordingLong();
			listRecording.setSessionId(sessionId);
			ListRecordingLongResponseCollection listRecordingShortResponseCollection = (ListRecordingLongResponseCollection)sasWebServiceTemplate.marshalSendAndReceiveToSAS("http://sas.elluminate.com/ListRecordingShort", listRecording);
			
			final List<RecordingLongResponse> recordingResponses = listRecordingShortResponseCollection.getRecordingLongResponses();
			
			for (final RecordingLongResponse recordingResponse : recordingResponses) {
			    final SessionRecording sessionRecording = sessionRecordingDao.createOrUpdateRecording(recordingResponse);
			    recordingList.add(sessionRecording);
			}
		}
        catch (Exception e)
        {
            //TODO this should get rethrown
            logger.error("Exception caught refreshing recordings",e);
        }
        return recordingList;
    }
}
