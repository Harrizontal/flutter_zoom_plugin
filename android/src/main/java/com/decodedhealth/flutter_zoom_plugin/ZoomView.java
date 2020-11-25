package com.decodedhealth.flutter_zoom_plugin;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.List;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;
import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKAuthenticationListener;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.InMeetingEventHandler;
import us.zoom.sdk.InMeetingChatMessage;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingAudioController.MobileRTCMicrophoneError;

public class ZoomView  implements PlatformView,
        MethodChannel.MethodCallHandler,
        ZoomSDKAuthenticationListener,
        InMeetingServiceListener{
    private final TextView textView;
    private final MethodChannel methodChannel;
    private final Context context;
    private final EventChannel meetingStatusChannel;

    ZoomView(Context context, BinaryMessenger messenger, int id) {
        textView = new TextView(context);
        textView.setAlpha(0f);
        this.context = context;

        methodChannel = new MethodChannel(messenger, "com.decodedhealth/flutter_zoom_plugin");
        methodChannel.setMethodCallHandler(this);

        meetingStatusChannel = new EventChannel(messenger, "com.decodedhealth/zoom_event_stream");


    }

    @Override
    public View getView() {
        return textView;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
        switch (methodCall.method) {
            case "init":
                init(methodCall, result);
                break;
            case "join":
                joinMeeting(methodCall, result);
                break;
            case "start":
                startMeeting(methodCall, result);
                break;
            case "meeting_status":
                meetingStatus(result);
                break;
            default:
                result.notImplemented();
        }

    }

    private void init(final MethodCall methodCall, final MethodChannel.Result result) {

        Map<String, String> options = methodCall.arguments();

        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        //ZoomSDK.getInstance().getInMeetingService().addListener(this);

        if(zoomSDK.isInitialized()) {
            List<Integer> response = Arrays.asList(0, 0);
            result.success(response);
            return;
        }

        ZoomSDKInitParams initParams = new ZoomSDKInitParams();
        initParams.jwtToken = options.get("sdkToken");
        initParams.appKey = options.get("appKey");
        initParams.appSecret = options.get("appSecret");
        initParams.domain = options.get("domain");
        zoomSDK.initialize(
                context,
                new ZoomSDKInitializeListener() {

                    @Override
                    public void onZoomAuthIdentityExpired() {

                    }

                    @Override
                    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
                        List<Integer> response = Arrays.asList(errorCode, internalErrorCode);

                        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
                            System.out.println("Failed to initialize Zoom SDK");
                            result.success(response);
                            return;
                        }

                        ZoomSDK zoomSDK = ZoomSDK.getInstance();
                        MeetingService meetingService = zoomSDK.getMeetingService();
                        meetingStatusChannel.setStreamHandler(new StatusStreamHandler(meetingService));
                        result.success(response);
                    }
                },
                initParams);
    }

    private void joinMeeting(MethodCall methodCall, MethodChannel.Result result) {

        Map<String, String> options = methodCall.arguments();

        String userId = options.get("userId");
        if(userId == null || userId.trim().isEmpty()) {
            /* do your stuffs here */
            userId = "Anonymous";
        }
        String nameAndEmailAddress = userId+",,,"+options.get("emailAddress");
        textView.setText(nameAndEmailAddress);
        //System.out.println(nameAndEmailAddress);
        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(!zoomSDK.isInitialized()) {
            System.out.println("Not initialized!!!!!!");
            result.success(false);
            return;
        }

        final MeetingService meetingService = zoomSDK.getMeetingService();


//        name = options.get("userId");
//        email = options.get("emailAddress");

        InMeetingService mInMeetingService = ZoomSDK.getInstance().getInMeetingService();
        mInMeetingService.addListener(this);

        JoinMeetingOptions opts = new JoinMeetingOptions();
        opts.no_invite = parseBoolean(options, "disableInvite", false);
        opts.no_share = parseBoolean(options, "disableShare", false);
        opts.no_driving_mode = parseBoolean(options, "disableDrive", false);
        opts.no_dial_in_via_phone = parseBoolean(options, "disableDialIn", false);
        opts.no_disconnect_audio = parseBoolean(options, "noDisconnectAudio", false);
        opts.no_audio = parseBoolean(options, "noAudio", false);
        opts.no_webinar_register_dialog = parseBoolean(options, "noWebinarRegisterDialog", true);;

        JoinMeetingParams params = new JoinMeetingParams();

        params.displayName = options.get("userId");
        params.meetingNo = options.get("meetingId");
        params.password = options.get("meetingPassword");

        meetingService.joinMeetingWithParams(context, params, opts);

        result.success(true);
    }

    private void startMeeting(MethodCall methodCall, MethodChannel.Result result) {

        Map<String, String> options = methodCall.arguments();

        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(!zoomSDK.isInitialized()) {
            System.out.println("Not initialized!!!!!!");
            result.success(false);
            return;
        }

        final MeetingService meetingService = zoomSDK.getMeetingService();

        StartMeetingOptions opts = new StartMeetingOptions();
        opts.no_invite = parseBoolean(options, "disableInvite", false);
        opts.no_share = parseBoolean(options, "disableShare", false);
        opts.no_driving_mode = parseBoolean(options, "disableDrive", false);
        opts.no_dial_in_via_phone = parseBoolean(options, "disableDialIn", false);
        opts.no_disconnect_audio = parseBoolean(options, "noDisconnectAudio", false);
        opts.no_audio = parseBoolean(options, "noAudio", false);

        StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();

		params.userId = options.get("userId");
        params.displayName = options.get("displayName");
        params.meetingNo = options.get("meetingId");
		params.userType = MeetingService.USER_TYPE_API_USER;
		params.zoomToken = options.get("zoomToken");
		params.zoomAccessToken = options.get("zoomAccessToken");
		
        meetingService.startMeetingWithParams(context, params, opts);

        result.success(true);
    }

    private boolean parseBoolean(Map<String, String> options, String property, boolean defaultValue) {
        return options.get(property) == null ? defaultValue : Boolean.parseBoolean(options.get(property));
    }


    private void meetingStatus(MethodChannel.Result result) {

        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if(!zoomSDK.isInitialized()) {
            System.out.println("Not initialized!!!!!!");
            result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "SDK not initialized"));
            return;
        }

        MeetingService meetingService = zoomSDK.getMeetingService();

        if(meetingService == null) {
            result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
            return;
        }

        MeetingStatus status = meetingService.getMeetingStatus();
        result.success(status != null ? Arrays.asList(status.name(), "") :  Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
    }

    @Override
    public void dispose() {}

    @Override
    public void onZoomAuthIdentityExpired() {

    }

    @Override
    public void onZoomSDKLoginResult(long result) {

    }

    @Override
    public void onZoomSDKLogoutResult(long result) {

    }

    @Override
    public void onZoomIdentityExpired() {

    }

    @Override
    public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler inMeetingEventHandler) {
        String name = textView.getText().toString();
        String[] array1 = name.split(",,,");
        inMeetingEventHandler.setRegisterWebinarInfo(array1[0],array1[1],false);
    }

    @Override
    public void onSinkAllowAttendeeChatNotification(int privilege) {

    }

    @Override
    public void onSinkAttendeeChatPriviledgeChanged(int privilege){

    }

    @Override
    public void onMeetingActiveVideo(long userId){

    }

    @Override
    public void onFreeMeetingReminder(boolean isHost, boolean canUpgrade, boolean isFirstGift){

    }

    @Override
    public void onSilentModeChanged(boolean a){

    }

    @Override
    public void onChatMessageReceived(InMeetingChatMessage inMeetingChatMessage){

    }

    @Override
    public void onMeetingSecureKeyNotification(byte[] asd){

    }

    @Override
    public void onLowOrRaiseHandStatusChanged(long a,boolean b){

    }

    @Override
    public void onMyAudioSourceTypeChanged(int a){

    }

    @Override
    public void onUserAudioTypeChanged(long a){

    }

    @Override
    public void onHostAskStartVideo(long a){

    }

    @Override
    public void onHostAskUnMute(long a ){

    }

    @Override
    public void onUserAudioStatusChanged(long a){

    }

    @Override
    public void onMicrophoneStatusError(MobileRTCMicrophoneError a){

    }

    @Override
    public void onUserNetworkQualityChanged(long a){

    }

    @Override
    public void onUserVideoStatusChanged(long a){

    }

    @Override
    public void onSpotlightVideoChanged(boolean a){

    }

    @Override
    public void onActiveSpeakerVideoUserChanged(long a){

    }

    @Override
    public void onActiveVideoUserChanged(long a){

    }

    @Override
    public void onMeetingCoHostChanged(long a){

    }

    @Override
    public void onMeetingHostChanged(long a){

    }

    @Override
    public void onMeetingUserUpdated(long a){

    }

    @Override
    public void onMeetingUserLeave(List<Long> a){

    }

    @Override
    public void onMeetingUserJoin(List<Long> a){

    }

    @Override
    public void onMeetingLeaveComplete(long a){

    }

    @Override
    public void onMeetingFail(int a ,int b){

    }

    @Override
    public void onMeetingNeedColseOtherMeeting(InMeetingEventHandler a){

    }

    @Override
    public void onWebinarNeedRegister(){

    }

    @Override
    public void onMeetingNeedPasswordOrDisplayName(boolean a ,boolean b,InMeetingEventHandler c){

    }

}
