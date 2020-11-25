
class ZoomOptions {

  String domain;
  String appKey;
  String appSecret;
  String jwtToken;

  ZoomOptions({
    this.domain,
    this.appKey,
    this.appSecret,
    this.jwtToken
  });
}

class ZoomMeetingOptions {

  String userId;
  String displayName;
  String emailAddress;
  String meetingId;
  String meetingPassword;
  String zoomToken;
  String zoomAccessToken;
  String disableDialIn;
  String disableDrive;
  String disableInvite;
  String disableShare;
  String noDisconnectAudio;
  String noAudio;
  String noWebinarRegisterDialog;

  ZoomMeetingOptions({
    this.userId,
    this.displayName,
    this.emailAddress,
    this.meetingId,
    this.meetingPassword,
    this.zoomToken,
    this.zoomAccessToken,
    this.disableDialIn,
    this.disableDrive,
    this.disableInvite,
    this.disableShare,
    this.noDisconnectAudio,
    this.noAudio,
    this.noWebinarRegisterDialog
  });
}
