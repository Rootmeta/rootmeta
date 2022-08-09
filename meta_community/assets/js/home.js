var app = new Vue({
	el: '#app',
	i18n: new VueI18n({
		locale: Store.get('locale') || 'en',
		dateTimeFormats: {
			en: {
				long: {
					year: 'numeric',
					month: 'long',
					day: 'numeric',
					hour: 'numeric',
					minute: 'numeric'
				}
			},
			zh: {
				long: {
					year: 'numeric',
					month: 'long',
					day: 'numeric',
					hour: 'numeric',
					minute: 'numeric'
				}
			}
		},
		messages: {
			en: en,
			zh: zh
		}
	}),
	data: {
		colors: ['#eb396d', '#ef834e', '#8ab0f9', '#66deb7', '#5b26f5'],
		activeFrame: null,
		user: null,
		notices: [],
		domainNotices: [],
		userFriendNotice: null,
		userTokenList: [],
		userNftList: [],
		userFriendList: [],
		userSetting: false,
		userSettingItem: 'account',//account
		userSettingAttrs: {
			userColor: '',
			userColorEdit: '',
			userBanner: '',
			userBannerEdit: '',
			aboutEdit: '',
			appearance: {
				style: 'comfortable',//comfortable,compact
			},
			notification: {
				inactiveDropdown: false
			},
			profileUnsaved: false
		},
		domainList: [],
		addDomainDialog: false,
		addDomain: {
			domainId: '',
			name: '',
			avatar: '',
			about: '',
			address: '',
			tagId: ''
		},
		whisperList: [],
		addWhisperDialog: false,
		addWhisper: {
			whisperId: '',
			name: '',
			searchText: ''
		},
		addWhisperUserDialog: false,
		addWhisperUser: {
			whisperId: '',
			name: '',
			searchText: ''
		},
		activePanel: 'friends',//friends,nitro,whisper
		friends: {
			friendStatus: 4,
			aliveStatus: 1,
			addUserFriend: false,
			searchFriendOnlineText: '',
			searchFriendAllText: '',
			searchFriendInvitingText: '',
			searchFriendRemoveText: '',
			searchUserText: '',
			userList: []
		},
		whisper: {
			whisperId: '',
			name: '',
			user: null,
			member: null,
			members: new Array(),
			memberList: [],
			messageList: [],
			messageLoading: false,
			showChatEmoji: false,
			showReactEmoji: false,
			reactPanelTop: 0,
			reactPanelRight: 0,
			reactMessage: null,
			replyMessage: null,
			editMessage: null,
			messaging: '',
		},
		userInfoDialog: false,
		userInfoAttrs: {
			infoTag: 'profile'//profile
		},
		userInfo: {
			user: null,
			userAbout: '',
			userTokenList: [],
			userNftList: [],
			userHistoryList: [],
			mutualDomains: [],
			isSelf: false,
			isFriend: false
		},
		userBaseDialog: false,
		userBase: {
			nickname: '',
			avatar: ''
		},
		userNicknameDialog: false,
		userNickname: {
			nickname: ''
		},
		userEmailDialog: false,
		userEmail: {
			email: ''
		},
		userAvatarDialog: false,
		userAvatar: {
			avatar: '',
			nftavatar: false
		},
		viewImage: false,
		viewImageUrl: '',
		viewImagePath: '',
		tagList: [],
		replyUser: null,
		ws: null,
		echoTimer: null
	},
	computed: {
		homeNotice: function () {
			var count = this.userFriendNotice ? this.userFriendNotice.count : 0;
			this.notices.forEach(function (notice) {
				count += notice.count;
			});
			return count;
		},
		addWhisperFriendList: function () {
			var list = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friendStatus.value == 4) {
					if (!this.addWhisper.searchText) {
						list.push(userFriend.friend);
					}
					else if (userFriend.friend.nickname.indexOf(this.addWhisper.searchText) >= 0) {
						list.push(userFriend.friend);
					}
				}
			}, this);
			return list;
		},
		addWhisperMemberList: function () {
			var list = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.addWhisperSelected) {
					list.push(userFriend.friend);
				}
			}, this);
			return list;
		},
		addWhisperUserFriendList: function () {
			var list = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friendStatus.value == 4 && !userFriend.friend.addWhisperSelected) {
					if (!this.addWhisper.searchText) {
						list.push(userFriend.friend);
					}
					else if (userFriend.friend.nickname.indexOf(this.addWhisper.searchText) >= 0) {
						list.push(userFriend.friend);
					}
				}
			}, this);
			return list;
		},
		addWhisperUserMemberList: function () {
			var list = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.addWhisperUserSelected) {
					list.push(userFriend.friend);
				}
			}, this);
			return list;
		},
		statusUserFriendList: function () {
			var list = [];
			this.userFriendList.forEach(function (userFriend) {
				if ((userFriend.friendStatus.value == 1 || userFriend.friendStatus.value == 2) && this.friends.friendStatus == 1) {
					if (!this.searchFriendInvitingText || userFriend.friend.nickname.indexOf(this.searchFriendInvitingText) >= 0) {
						list.push(userFriend);
					}
				}
				else if (userFriend.friendStatus.value == 4 && this.friends.friendStatus == 4) {
					if (this.friends.aliveStatus == 0 && 
							(!this.searchFriendAllText || userFriend.friend.nickname.indexOf(this.searchFriendAllText) >= 0)) {
						list.push(userFriend);
					}
					else if (this.friends.aliveStatus == 1 && userFriend.friend.aliveStatus.value == 1 && 
							(!this.searchFriendOnlineText || userFriend.friend.nickname.indexOf(this.searchFriendOnlineText) >= 0)) {
						list.push(userFriend);
					}
				}
				else if (userFriend.friendStatus.value == 16 && this.friends.friendStatus == 16) {
					if (!this.searchFriendRemoveText || userFriend.friend.nickname.indexOf(this.searchFriendRemoveText) >= 0) {
						list.push(userFriend);
					}
				}
			}, this);
			return list;
		},
		//self
		userChainBalances: function () {
			var map = new Array();
			var list = [];
			this.userTokenList.forEach(function (userToken) {
				var chainBalance = map[userToken.chainId];
				if (!chainBalance) {
					chainBalance = {
						id: userToken.chain.chainId,
						name: userToken.chain.name,
						logo: userToken.chain.logo,
						amount: 0
					};
					map[userToken.chainId] = chainBalance;
					list.push(chainBalance);
				}
				chainBalance.amount += userToken.amount;
			});
			return list;
		},
		//anyone
		chainBalances: function () {
			var map = new Array();
			var list = [];
			this.userInfo.userTokenList.forEach(function (userToken) {
				var chainBalance = map[userToken.chainId];
				if (!chainBalance) {
					chainBalance = {
						id: userToken.chain.chainId,
						name: userToken.chain.name,
						logo: userToken.chain.logo,
						amount: 0
					};
					map[userToken.chainId] = chainBalance;
					list.push(chainBalance);
				}
				chainBalance.amount += userToken.amount;
			});
			return list;
		}
	},
	mounted: function () {
		if (!Store.get('client')) {
			Api.client(this, null, function (rspBody) {
				Store.set('client', rspBody.client);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		}
		
		this.loadUser(function () {
			this.loadUserDomainList();
			this.loadUserFriendList();
			this.loadUserWhisperList(function () {
				var whisperId = Url.decode(Url.hash('wid'));
				var whisper = null;
				this.whisperList.forEach(function (w) {
					if (w.whisperId == whisperId) {
						whisper = w;
						return false;
					}
				});
				if (whisper) {
					this.activePanel = 'whisper';
					this.whisper.whisperId = whisper.whisperId;
					this.whisper.name = whisper.name;
					this.loadWhisperUserList(this.whisper.whisperId);
					this.loadWhisperMessageList(this.whisper.whisperId, null, null, function () {
						this.msgListScroll();
						
						this.loadWhisperMessageNotices();
					});
				}
				else {
					this.loadWhisperMessageNotices();
				}
			});
			
			this.loadTeamMessageNotices();
			this.loadUserFriendNotice();
			
			this.loadTagList();
			this.userBase.nickname = this.user.nickname;
			this.userBase.avatar = this.user.avatar;
			if (this.user.inited.value == 0) {
				this.userBaseDialog = true;
			}
		});
		
		this.connect();
		var _this = this;
		setInterval(function () {
			if (_this.ws == null || _this.ws.readyState == WebSocket.CLOSING || _this.ws.readyState == WebSocket.CLOSED) {
				_this.connect();
			}
		}, 1000);
		
		ethereum.on('accountsChanged', function (accounts) {
			const account = accounts[0];
			
			Api.swap(_this, {
				address: account
			}, function (rspBody) {
				var uticket = rspBody.uticket;
				Store.set('uticket', uticket);
				
				//reinit
				_this.loadUser(function () {
					_this.loadUserDomainList();
					_this.loadUserFriendList();
					_this.loadUserWhisperList(function () {
						if (_this.whisperList.length > 0) {
							var whisper = _this.whisperList[0];
							_this.whisper.whisperId = whisper.whisperId;
							_this.whisper.name = whisper.name;
							_this.loadWhisperUserList(_this.whisper.whisperId);
							_this.loadWhisperMessageList(_this.whisper.whisperId, null, null, function () {
								_this.msgListScroll();
								
								_this.loadTeamMessageNotices();
								_this.loadWhisperMessageNotices();
								_this.loadUserFriendNotice();
							});
						}
						else {
							_this.whisper.whisperId = '';
							_this.whisper.name = '';
							_this.activePanel = 'friends';
							
							_this.loadTeamMessageNotices();
							_this.loadWhisperMessageNotices();
							_this.loadUserFriendNotice();
						}
					});
					_this.userBase.nickname = _this.user.nickname;
					_this.userBase.avatar = _this.user.avatar;
					if (_this.user.inited.value == 0) {
						_this.userBaseDialog = true;
					}
				});
				
				_this.connect();
			}, function (code, message) {
				MsgBox.alert(message);
			});
		});
	},
	methods: {
		connect: function () {
			if (this.ws) {
				this.ws.close();
			}
			if (this.echoTimer) {
				clearInterval(this.echoTimer);
			}
			this.ws = new WebSocket("wss://ws.rootmeta.xyz/ws");
			var _this = this;
			this.ws.onopen = function () {
				var echo = {
					uticket: Store.get('uticket'),
					client: Store.get('client')
				};
				_this.ws.send(JSON.stringify(echo));
				_this.echoTimer = setInterval(function () {
					if (_this.ws.readyState == WebSocket.OPEN) {
						_this.ws.send(JSON.stringify(echo));
					}
				}, 45000);
			};
			this.ws.onmessage = function (e) {
				var data = JSON.parse(e.data);
				if (data.account != _this.user.userId || data.client != Store.get('client')) {
					return;
				}
				var message = data.dataContent;
				if (data.dataType.value == 1) {
					//TODO:
				}
				else if (data.dataType.value == 2) {
					//team_message
					var domainNotice = null;
					_this.domainNotices.forEach(function (idomainNotice) {
						if (idomainNotice.domainId == message.domainId) {
							domainNotice = idomainNotice;
							return false;
						}
					});
					if (!domainNotice) {
						domainNotice = {
							domainId: message.domainId,
							count: 0,
							teamNotices: []
						};
						_this.domainNotices.push(domainNotice);
					}
					domainNotice.count = domainNotice.count + 1;
					var teamNotice = null;
					domainNotice.teamNotices.forEach(function (iteamNotice) {
						if (iteamNotice.teamId == message.teamId) {
							teamNotice = iteamNotice;
							return false;
						}
					});
					if (!teamNotice) {
						teamNotice = {
							teamId: message.teamId,
							count: 0
						};
						domainNotice.teamNotices.push(teamNotice);
					}
					teamNotice.count = teamNotice.count + 1;
					
					_this.domainList.forEach(function (idomain) {
						if (idomain.domainId == message.domainId) {
							idomain.noticeCount = idomain.noticeCount + 1;
							return false;
						}
					});
				}
				else if (data.dataType.value == 4) {
					//whisper_message
					if (message.whisperId == _this.whisper.whisperId) {
						Api.loadWhisperMessage(_this, {
							messageId: message.messageId
						}, function (rspBody) {
							var msg = rspBody.message;
							_this.whisper.messageList.push(msg);
							_this.msgListScroll();
							Api.acceptWhisperMessageNotice(_this, {
								whisperId: message.whisperId
							});
						}, function (code, message) {
							MsgBox.alert(message);
						});
					}
					else {
						var notice = null;
						_this.notices.forEach(function (inotice) {
							if (inotice.whisperId == message.whisperId) {
								notice = inotice;
								return false;
							}
						});
						if (!notice) {
							notice = {
								whisperId: message.whisperId,
								count: 0
							};
							_this.notices.push(notice);
						}
						notice.count = notice.count + 1;
						
						_this.whisperList.forEach(function (iwhisper) {
							if (iwhisper.whisperId == message.whisperId) {
								iwhisper.noticeCount = iwhisper.noticeCount + 1;
								return false;
							}
						});
					}
				}
				else if (data.dataType.value == 12) {
					if (!_this.userFriendNotice) {
						_this.userFriendNotice = {
							userId: _this.user.userId,
							count: 0
						};
					}
					_this.userFriendNotice.count = _this.userFriendNotice.count + 1;
					
					_this.userFriendList.push(message);
				}
			};
			this.ws.oneror = function () {
				//
			};
			this.ws.onclose = function () {
				//
			};
		},
		formatMessage: function (textContent) {
			return textContent.replace(/\r\n|\r|\n/gi, '<br/>');
		},
		formatAmount: function (amount) {
			return amount.toFixed(2).replace(/\d(?=(?:\d{3})+\b)/g, '$&,');
		},
		formatNumber: function (amount) {
			return String(amount).replace(/\d(?=(?:\d{3})+\b)/g, '$&,');
		},
		hideMsgBox: function (e) {
			e.stopPropagation();
			MsgBox.hide();
		},
		clickContainer: function (e) {
			if (this.activeFrame != 'addDomainDialog') {this.addDomainDialog = false;}
			if (this.activeFrame != 'addWhisperDialog') {this.addWhisperDialog = false;}
			if (this.activeFrame != 'userInfoDialog') {this.userInfoDialog = false;}
			if (this.activeFrame != 'userBaseDialog') {this.userBaseDialog = false;}
			if (this.activeFrame != 'userNicknameDialog') {this.userNicknameDialog = false;}
			if (this.activeFrame != 'userEmailDialog') {this.userEmailDialog = false;}
			if (this.activeFrame != 'userAvatarDialog') {this.userAvatarDialog = false;}
			if (this.activeFrame != 'showChatEmoji') {this.whisper.showChatEmoji = false;}
			if (this.activeFrame != 'showReactEmoji') {this.whisper.showReactEmoji = false;}
			this.userSettingAttrs.notification.inactiveDropdown = false;
			this.activeFrame = null;
		},
		clickLanguage: function (locale) {
			this.$i18n.locale = locale;
			Store.set('locale', locale);
		},
		clickShowFrame: function (frame) {
			this.activeFrame = frame;
		},
		clickHome: function () {
			//
		},
		clickTweets: function () {
			window.location = '/tweets.html';
		},
		clickExploreDomain: function () {
			window.location = '/explore.html';
		},
		clickDomain: function (domainId) {
			window.location = '/server/' + Url.encode(domainId);
		},
		clickAddDomain: function () {
			this.addDomain.domainId = '';
			this.addDomain.name = '';
			this.addDomain.avatar = '';
			this.addDomain.about = '';
			this.addDomain.address = '';
			this.addDomain.tagId = '';
			this.addDomainDialog = true;
			window.event.stopPropagation();
		},
		changeAddDomainAvatar: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				this.addDomain.avatar = result.url;
			}, function (message) {
				MsgBox.alert(message);
			});
		},
		clickAddDomainConfirm: function () {
			this.createDomain(function (domain, category, team) {
				window.location = '/server/' + Url.encode(domain.domainId);
			});
		},
		clickAddDomainCancel: function () {
			this.addDomainDialog = false;
		},
		clickAddDomainTag: function (tagId) {
			this.addDomain.tagId = tagId;
		},
		clickAddWhisper: function () {
			this.addWhisper.whisperId = '';
			this.addWhisper.name = '';
			this.addWhisper.searchText = '';
			this.userFriendList.forEach(function (userFriend) {
				userFriend.friend.addWhisperSelected = false;
			});
			this.addWhisperDialog = true;
			window.event.stopPropagation();
		},
		clickAddWhisperConfirm: function () {
			var memberIds = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.addWhisperSelected) {
					memberIds.push(userFriend.friend.userId);
				}
			});
			this.createWhisper(memberIds, function (whisper) {
				this.addWhisperDialog = false;
				this.loadUserWhisperList();
				this.activePanel = 'whisper';
				this.whisper.whisperId = whisper.whisperId;
				this.whisper.name = whisper.name;
				this.loadWhisperUserList(this.whisper.whisperId);
				this.loadWhisperMessageList(this.whisper.whisperId, null, null, function () {
					this.msgListScroll();
				});
			});
		},
		clickAddWhisperCancel: function () {
			this.addWhisperDialog = false;
		},
		clickAddWhisperAddMember: function (userId) {
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.userId == userId) {
					userFriend.friend.addWhisperSelected = true;
					return false;
				}
			});
		},
		clickAddWhisperRemoveMember: function (userId) {
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.userId == userId) {
					userFriend.friend.addWhisperSelected = false;
					return false;
				}
			});
		},
		clickAddWhisperUser: function (whisperId) {
			this.clickPanel('whisper', whisperId, function () {
				this.addWhisperUser.whisperId = this.whisper.whisperId;
				this.addWhisperUser.name = this.whisper.name;
				this.addWhisperUser.searchText = '';
				this.userFriendList.forEach(function (userFriend) {
					userFriend.friend.addWhisperSelected = this.whisper.members[userFriend.friend.userId] ? true : false;
					userFriend.friend.addWhisperUserSelected = false;
				}, this);
				this.addWhisperUserDialog = true;
			});
			window.event.stopPropagation();
		},
		clickAddWhisperUserConfirm: function () {
			var memberIds = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.addWhisperUserSelected) {
					memberIds.push(userFriend.friend.userId);
				}
			});
			if (this.whisper.member) {
				this.createWhisper(memberIds, function (whisper) {
					this.addWhisperUserDialog = false;
					this.activePanel = 'whisper';
					this.loadUserWhisperList(function () {
						this.whisperList.forEach(function (iwhisper) {
							if (iwhisper.whisperId == whisper.whisperId) {
								this.whisper.whisperId = iwhisper.whisperId;
								this.whisper.name = iwhisper.name;
								this.whisper.user = iwhisper.user || null;
								this.whisper.member = iwhisper.member || null;
								return false;
							}
						}, this);
						this.loadWhisperUserList(this.whisper.whisperId);
						this.loadWhisperMessageList(this.whisper.whisperId, null, null, function () {
							this.msgListScroll();
						});
					});
				});
			}
			else {
				this.joinWhisperUsers(this.whisper.whisperId, memberIds, function (whisper) {
					this.addWhisperUserDialog = false;
					this.whisperList.forEach(function (iwhisper) {
						if (iwhisper.whisperId == whisper.whisperId) {
							iwhisper.memberCount = whisper.memberCount;
							return false;
						}
					}, this);
					this.loadWhisperUserList(this.whisper.whisperId);
				});
			}
		},
		clickAddWhisperUserCancel: function () {
			this.addWhisperUserDialog = false;
		},
		clickAddWhisperUserAddMember: function (userId) {
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.userId == userId) {
					userFriend.friend.addWhisperUserSelected = true;
					return false;
				}
			});
		},
		clickAddWhisperUserRemoveMember: function (userId) {
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friend.userId == userId) {
					userFriend.friend.addWhisperUserSelected = false;
					return false;
				}
			});
		},
		clickMe: function () {
			this.clickUserInfo(this.user.userId);
		},
		clickUserSetting: function () {
			this.userSetting = true;
			window.event.stopPropagation();
		},
		clickUserSettingItem: function (userSettingItem) {
			if (userSettingItem != 'logout') {
				this.userSettingItem = userSettingItem;
				return;
			}
			//quit
			Api.quit(this, null, function (rspBody) {
				Store.set('uticket', '');
				window.location = '/index.html';
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickEditUserProfile: function () {
			this.userSettingItem = 'profile';
		},
		clickEditUserNickname: function () {
			this.userNicknameDialog = true;
			window.event.stopPropagation();
		},
		clickEditUserEmail: function () {
			this.userEmailDialog = true;
			window.event.stopPropagation();
		},
		clickChangeUserAvatar: function () {
			this.userAvatarDialog = true;
			window.event.stopPropagation();
		},
		clickChangeUserColorDefault: function () {
			if (this.userSettingAttrs.userColorEdit) {
				this.userSettingAttrs.userColorEdit = '';
				this.userSettingAttrs.profileUnsaved = true;
			}
		},
		clickChangeUserColor: function (userColor) {
			this.userSettingAttrs.userColorEdit = userColor;
			this.userSettingAttrs.profileUnsaved = true;
		},
		changeChangeUserBanner: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				this.userSettingAttrs.userBannerEdit = result.url;
			}, function (message) {
				MsgBox.alert(message);
			});
			this.userSettingAttrs.profileUnsaved = true;
		},
		inputUserAbout: function () {
			this.userSettingAttrs.profileUnsaved = true;
		},
		clickUserSettingSaveProfile: function () {
			Api.edit(this, {
				nickname: this.user.nickname,
				email: this.user.email,
				avatar: this.user.avatar,
				banner: this.userSettingAttrs.userBannerEdit,
				color: this.userSettingAttrs.userColorEdit,
				nftavatar: this.user.nftavatar.value,
				about: this.userSettingAttrs.aboutEdit
			}, function (rspBody) {
				this.userSettingAttrs.profileUnsaved = false;
				this.user = rspBody.user;
				this.userSettingAttrs.userBanner = this.user.banner;
				this.userSettingAttrs.userBannerEdit = this.user.banner;
				this.userSettingAttrs.userColor = this.user.color;
				this.userSettingAttrs.userColorEdit = this.user.color;
				this.userSettingAttrs.aboutEdit = this.user.about;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickUserSettingResetProfile: function () {
			this.userSettingAttrs.userBannerEdit = this.user.banner;
			this.userSettingAttrs.userColorEdit = this.user.color;
			this.userSettingAttrs.aboutEdit = this.user.about;
			this.userSettingAttrs.profileUnsaved = false;
		},
		clickUserSettingCancel: function () {
			this.userSetting = false;
		},
		clickUserFriendStatus: function (friendStatus, aliveStatus) {
			this.friends.friendStatus = friendStatus;
			this.friends.aliveStatus = aliveStatus;
			this.friends.addUserFriend = false;
			if (friendStatus == 1) {
				if (this.userFriendNotice && this.userFriendNotice.count) {
					Api.acceptUserFriendNotice(this, null, function (rspBody) {
						//
					}, function (code, message) {
						MsgBox.alert(message);
					});
					this.userFriendNotice.count = 0;
				}
			}
		},
		clickAddUserFriend: function () {
			this.friends.friendStatus = 0;
			this.friends.aliveStatus = 0;
			this.friends.addUserFriend = true;
		},
		clickAddUserFriendInvite: function (userId) {
			window.event.stopPropagation();
			this.inviteUserFriend(userId, function () {
				MsgBox.alert('好友邀请已发送', '提示');
				this.loadUserFriendList();
			});
		},
		clickAddUserFriendSendMessage: function (userId) {
			window.event.stopPropagation();
			this.clickUserInfoSendMessage(userId);
		},
		clickCancelUserFriend: function (userId) {
			window.event.stopPropagation();
			this.cancelUserFriend(userId, function () {
				this.loadUserFriendList();
			});
		},
		clickAcceptUserFriend: function (userId) {
			window.event.stopPropagation();
			this.acceptUserFriend(userId, function () {
				this.loadUserFriendList();
			});
		},
		clickRejectUserFriend: function (userId) {
			window.event.stopPropagation();
			this.rejectUserFriend(userId, function () {
				this.loadUserFriendList();
			});
		},
		clickRemoveUserFriend: function (userId) {
			window.event.stopPropagation();
			this.removeUserFriend(userId, function () {
				this.loadUserFriendList();
			});
		},
		clickSendUserFriendMessage: function (userId) {
			window.event.stopPropagation();
			if (!userId) {
				return;
			}
			this.promiseWhisper(userId, function (whisper) {
				this.loadUserWhisperList();
				this.activePanel = 'whisper';
				this.whisper.whisperId = whisper.whisperId;
				this.whisper.name = whisper.name;
				this.loadWhisperUserList(this.whisper.whisperId);
				this.loadWhisperMessageList(this.whisper.whisperId, null, null, function () {
					this.msgListScroll();
				});
				this.clickContainer();
			});
		},
		clickNotificationInactiveDropdown: function () {
			this.userSettingAttrs.notification.inactiveDropdown = !this.userSettingAttrs.notification.inactiveDropdown;
			window.event.stopPropagation();
		},
		clickPanel: function (activePanel, whisperId, cb) {
			this.activePanel = activePanel;
			if (this.activePanel == 'whisper') {
				if (this.whisper.whisperId == whisperId) {
					cb && cb.call(this);
					return;
				}
				var whisper = null;
				this.whisperList.forEach(function (wsp) {
					if (wsp.whisperId == whisperId) {
						whisper = wsp;
						return false;
					}
				});
				this.whisperList.forEach(function (iwhisper) {
					if (iwhisper.whisperId == whisperId) {
						this.whisper.whisperId = iwhisper.whisperId;
						this.whisper.name = iwhisper.name;
						this.whisper.user = iwhisper.user || null;
						this.whisper.member = iwhisper.member || null;
						return false;
					}
				}, this);
				this.loadWhisperUserList(this.whisper.whisperId, function () {
					cb && cb.call(this);
				});
				this.loadWhisperMessageList(this.whisper.whisperId, null, null, function () {
					this.msgListScroll();
				});
				if (this.whisper.member) {
					this.loadUserInfo(this.whisper.member.userId);
				}
			}
		},
		clickUserInfo: function (userId) {
			this.userInfoAttrs.infoTag = 'assets';
			this.loadUserInfo(userId, function () {
				this.loadUserMutualDomains(userId);
				this.userInfoDialog = true;
			});
			window.event.stopPropagation();
		},
		clickUserInfoTab: function (userInfoTab) {
			this.userInfoAttrs.infoTag = userInfoTab;
		},
		clickUserInfoSendMessage: function (userId) {
			if (!userId) {
				return;
			}
			this.promiseWhisper(userId, function (whisper) {
				this.activePanel = 'whisper';
				this.loadUserWhisperList(function () {
					this.whisperList.forEach(function (iwhisper) {
						if (iwhisper.whisperId == whisper.whisperId) {
							this.whisper.whisperId = iwhisper.whisperId;
							this.whisper.name = iwhisper.name;
							this.whisper.user = iwhisper.user || null;
							this.whisper.member = iwhisper.member || null;
							return false;
						}
					}, this);
					this.loadWhisperUserList(this.whisper.whisperId);
					this.loadWhisperMessageList(this.whisper.whisperId, null, null, function () {
						this.msgListScroll();
					});
				});
				this.clickContainer();
			});
		},
		clickUserInfoInvite: function (userId) {
			if (!userId) {
				return;
			}
			this.inviteUserFriend(userId, function () {
				MsgBox.alert('Invite Request Sent', 'Info');
				this.loadUserFriendList();
			});
		},
		clickUserInfoMutualDomain: function (domainId) {
			window.location = '/server/' + Url.encode(domainId);
		},
		changeUserBaseAvatar: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				this.userBase.avatar = result.url;
			}, function (message) {
				MsgBox.alert(message);
			});
		},
		clickUserBaseConfirm: function () {
			Api.init(this, {
				nickname: this.userBase.nickname,
				avatar: this.userBase.avatar
			}, function (rspBody) {
				this.userBaseDialog = false;
				this.user.nickname = rspBody.user.nickname;
				this.user.avatar = rspBody.user.avatar;
				this.user.inited = rspBody.user.inited;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickUserBaseCancel: function () {
			this.userBaseDialog = false;
		},
		clickUserNicknameConfirm: function () {
			Api.edit(this, {
				nickname: this.userNickname.nickname,
				email: this.user.email,
				avatar: this.user.avatar,
				color: this.user.color,
				nftavatar: this.user.nftavatar.value,
				about: this.user.about
			}, function (rspBody) {
				this.userNicknameDialog = false;
				this.user = rspBody.user;
				this.userNickname.nickname = this.user.nickname;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickUserNicknameCancel: function () {
			this.userNicknameDialog = false;
		},
		clickUserEmailConfirm: function () {
			Api.edit(this, {
				nickname: this.user.nickname,
				email: this.userEmail.email,
				avatar: this.user.avatar,
				color: this.user.color,
				nftavatar: this.user.nftavatar.value,
				about: this.user.about
			}, function (rspBody) {
				this.userEmailDialog = false;
				this.user = rspBody.user;
				this.userEmail.email = this.user.email;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickUserEmailCancel: function () {
			this.userEmailDialog = false;
		},
		clickUserAvatarConfirm: function () {
			Api.edit(this, {
				nickname: this.user.nickname,
				email: this.user.email,
				avatar: this.userAvatar.avatar,
				color: this.user.color,
				nftavatar: this.userAvatar.nftavatar,
				about: this.user.about
			}, function (rspBody) {
				this.userAvatarDialog = false;
				this.user = rspBody.user;
				this.userAvatar.avatar = this.user.avatar;
				this.userAvatar.nftavatar = this.user.nftavatar.value;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickUserAvatarCancel: function () {
			this.userAvatarDialog = false;
		},
		changeUserAvatarAvatar: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				this.userAvatar.avatar = result.url;
				this.userAvatar.nftavatar = 0;
			}, function (message) {
				MsgBox.alert(message);
			});
		},
		clickUserNft: function (nftUrl) {
			this.userAvatar.avatar = nftUrl;
			this.userAvatar.nftavatar = 1;
		},
		keypressAddUserFriendSearch: function (e) {
			if (e.keyCode != 13) {
				return;
			}
			Api.loadUserList(this, {
				nickname: this.friends.searchUserText
			}, function (rspBody) {
				rspBody.userList.forEach(function (user) {
					var isFriend = false;
					this.userFriendList.forEach(function (userFriend) {
						if (userFriend.friendStatus.value == 4 && userFriend.friend.userId == user.userId) {
							isFriend = true;
							return false;
						}
					});
					user.isFriend = isFriend;
				}, this);
				this.friends.userList = rspBody.userList;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		scrollMsgList: function (e) {
			if (e.target.scrollTop <= 150) {
				if (this.messageLoading) {
					return;
				}
				var scrollTop = e.target.scrollTop;
				var scrollHeight = e.target.scrollHeight;
				this.messageLoading = true;
				this.loadWhisperMessageList(this.whisper.whisperId, null, (this.whisper.messageList,length ? this.whisper.messageList[0].messageId : null), function () {
					this.$nextTick(function () {
						var msgList = this.$el.querySelector('#msg #list');
						msgList.scrollTop = msgList.scrollHeight - (scrollHeight - scrollTop);
					});
				});
			}
			else {
				if (this.messageLoading) {
					this.messageLoading = false;
				}
			}
		},
		clickViewImage: function (url) {
			this.viewImageUrl = url;
			var path = url;
			if (url.indexOf('http://') == 0) {
				var index = url.indexOf('/', 'http://'.length);
				path = '.' + url.substring(index);
			}
			else if (url.indexOf('https://') == 0) {
				var index = url.indexOf('/', 'https://'.length);
				path = '.' + url.substring(index);
			}
			this.viewImagePath = path;
			this.viewImage = true;
		},
		clickMsgJoinDomain: function (domainId) {
			Api.joinDomain(this, {
				domainId: domainId
			}, function (rspBody) {
				window.location = '/server/' + Url.encode(domainId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickMsgReact: function (messageId, emoji) {
			Api.reactTeamMessage(this, {
				messageId: messageId,
				emojiContent: emoji
			}, function (rspBody) {
				this.whisper.messageList.forEach(function (imsg) {
					if (imsg.messageId == messageId) {
						imsg.reactionList = rspBody.message.reactionList;
						return false;
					}
				});
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickReactEmojiItem: function (emoji) {
			Api.reactWhisperMessage(this, {
				messageId: this.whisper.reactMessage.messageId,
				emojiContent: emoji
			}, function (rspBody) {
				this.whisper.reactMessage.reactionList = rspBody.message.reactionList;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickMessageOperReact: function (messageId) {
			this.whisper.replyMessage = null;
			this.whisper.editMessage = null;
			this.whisper.messageList.forEach(function (imessage) {
				if (imessage.messageId == messageId) {
					this.whisper.reactMessage = imessage;
					return false;
				}
			}, this);
			var width = window.innerWidth;
			var height = window.innerHeight;
			var x = window.event.pageX - window.event.offsetX;
			var y = window.event.pageY - window.event.offsetY;
			if (height - y < 260 + 10) {
				this.whisper.reactPanelTop = height - 260 - 10;
			}
			else {
				this.whisper.reactPanelTop = y - 10;
			}
			this.whisper.reactPanelRight = width - x + 10;
			this.whisper.showReactEmoji = !this.whisper.showReactEmoji;
			window.event.stopPropagation();
		},
		clickMessageOperReply: function (messageId) {
			this.whisper.reactMessage = null;
			this.whisper.editMessage = null;
			this.whisper.messageList.forEach(function (imessage) {
				if (imessage.messageId == messageId) {
					this.whisper.replyMessage = imessage;
					return false;
				}
			}, this);
			this.$el.querySelector('.msg .send .inputs-holder .textbox').focus();
		},
		clickMessageOperReplyCancel: function () {
			this.whisper.replyMessage = null;
		},
		clickMessageOperEdit: function (messageId) {
			this.whisper.reactMessage = null;
			this.whisper.replyMessage = null;
			this.whisper.messageList.forEach(function (imessage) {
				if (imessage.messageId == messageId) {
					this.whisper.editMessage = imessage;
					return false;
				}
			}, this);
			var textbox = this.$el.querySelector('.msg .send .inputs-holder .textbox');
			textbox.innerText = this.whisper.editMessage.textContent;
			textbox.focus();
		},
		clickMessageOperEditCancel: function () {
			this.whisper.editMessage = null;
		},
		clickMessageOperRevoke: function (messageId) {
			Api.revokeWhisperMessage(this, {
				messageId: messageId
			}, function (rspBody) {
				for (var i = 0; i < this.whisper.messageList.length; i++) {
					var imessage = this.whisper.messageList[i];
					if (imessage.messageId == messageId) {
						this.whisper.messageList.splice(i, 1);
						break;
					}
				}
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		changeSendFile: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				var filename = result.originalName;
				var index = filename.lastIndexOf('.');
				var ext = '';
				if (index > 0) {
					ext = filename.substring(index + 1).toLocaleLowerCase();
				}
				var messageType = 8;
				if (ext == 'jpg' || ext == 'jpeg' || ext == 'png' || ext == 'gif' || ext == 'webp') {
					messageType = 2;
				}
				else if (ext == 'mp3') {
					messageType = 4;
				}
				else if (ext == 'mp4') {
					messageType = 6;
				}
				Api.sendWhisperMessage(this, {
					whisperId: this.whisper.whisperId,
					messageType: messageType,
					url: result.url,
					replyMessageId: this.whisper.replyMessage ? this.whisper.replyMessage.messageId : ''
				}, function (rspBody) {
					this.whisper.reactMessage = null;
					this.whisper.replyMessage = null;
					this.whisper.editMessage = null;
				}, function (code, message) {
					MsgBox.alert(message);
				});
			}, function (message) {
				MsgBox.alert(message);
			});
		},
		keydownMessage: function (e) {
			this.whisper.messaging = e.keyCode;
			if (e.keyCode == 13 && !e.shiftKey) {
				e.preventDefault();
			}
		},
		keyupMessage: function (e) {
			var textContent = e.target.innerText;
			if (e.keyCode != 13 || e.shiftKey || this.whisper.messaging != e.keyCode) {
				return;
			}
			if (!textContent) {
				return;
			}
			if (!this.whisper.editMessage) {
				Api.sendWhisperMessage(this, {
					whisperId: this.whisper.whisperId,
					messageType: 1,
					textContent: textContent,
					replyMessageId: this.whisper.replyMessage ? this.whisper.replyMessage.messageId : ''
				}, function (rspBody) {
					e.target.innerText = '';
					this.whisper.reactMessage = null;
					this.whisper.replyMessage = null;
					this.whisper.editMessage = null;
				}, function (code, message) {
					e.target.innerText = '';
					MsgBox.alert(message);
				});
			}
			else {
				Api.editWhisperMessage(this, {
					messageId: this.whisper.editMessage.messageId,
					textContent: textContent
				}, function (rspBody) {
					e.target.innerText = '';
					this.whisper.editMessage.textContent = rspBody.message.textContent;
					this.whisper.editMessage = null;
				}, function (code, message) {
					e.target.innerText = '';
					MsgBox.alert(message);
				});
			}
		},
		clickChatEmoji: function () {
			this.whisper.showChatEmoji = !this.whisper.showChatEmoji;
			window.event.stopPropagation();
		},
		clickChatEmojiItem: function (emoji) {
			var textbox = document.querySelector('.send .inputs-holder .textbox');
			textbox.focus();
			textbox.innerText += emoji;
			this.whisper.showChatEmoji = false;
		},
		clickViewImageClose: function () {
			this.viewImage = false;
		},
		clickCopyMyAddress: function () {
			document.querySelector('#myadd input').select();
			document.execCommand('copy');
		},
		clickCopyFriendAddress: function () {
			document.querySelector('#fradd input').select();
			document.execCommand('copy');
		},
		loadUser: function (onsuccess, onfailed) {
			Api.loadUser(this, {
				withAsset: true
			}, function (rspBody) {
				var chains = new Array();
				rspBody.chains.forEach(function (chain) {
					chains[chain.chainId] = chain;
				});
				var tokens = new Array();
				rspBody.tokens.forEach(function (token) {
					tokens[token.tokenId] = token;
				});
				rspBody.userTokenList.forEach(function (userToken) {
					userToken.chain = chains[userToken.chainId];
					userToken.token = tokens[userToken.tokenId];
				});
				rspBody.userNftList.forEach(function (userNft) {
					userNft.chain = chains[userNft.chainId];
				});
				this.user = rspBody.user;
				this.userNickname.nickname = this.user.nickname;
				this.userEmail.email = this.user.email;
				this.userAvatar.avatar = this.user.avatar;
				this.userAvatar.nftavatar = this.user.nftavatar.value;
				this.userSettingAttrs.userBanner = this.user.banner;
				this.userSettingAttrs.userBannerEdit = this.user.banner;
				this.userSettingAttrs.userColor = this.user.color;
				this.userSettingAttrs.userColorEdit = this.user.color;
				this.userSettingAttrs.aboutEdit = this.user.about;
				this.userTokenList = rspBody.userTokenList;
				this.userNftList = rspBody.userNftList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserInfo: function (userId, onsuccess, onfailed) {
			Api.loadUserInfo(this, {
				userId: userId,
				withAsset: true
			}, function (rspBody) {
				var chains = new Array();
				rspBody.chains.forEach(function (chain) {
					chains[chain.chainId] = chain;
				});
				var tokens = new Array();
				rspBody.tokens.forEach(function (token) {
					tokens[token.tokenId] = token;
				});
				rspBody.userTokenList.forEach(function (userToken) {
					userToken.chain = chains[userToken.chainId];
					userToken.token = tokens[userToken.tokenId];
				});
				rspBody.userNftList.forEach(function (userNft) {
					userNft.chain = chains[userNft.chainId];
				});
				rspBody.userHistoryList.forEach(function (userHistory) {
					userHistory.chain = chains[userHistory.chainId];
					userHistory.token = tokens[userHistory.tokenId];
					userHistory.gasToken = tokens[userHistory.gasTokenId];
				});
				this.userInfo.user = rspBody.user;
				this.userInfo.userAbout = rspBody.user != null ? rspBody.user.about : '';
				this.userInfo.userTokenList = rspBody.userTokenList;
				this.userInfo.userNftList = rspBody.userNftList;
				this.userInfo.userHistoryList = rspBody.userHistoryList;
				var isSelf = (this.user.userId == userId);
				var isFriend = false;
				this.userFriendList.forEach(function (userFriend) {
					if (userFriend.friendStatus.value == 4 && userFriend.friend.userId == userId) {
						isFriend = true;
						return false;
					}
				});
				this.userInfo.isSelf = isSelf;
				this.userInfo.isFriend = isFriend;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadTeamMessageNotices: function (onsuccess, onfailed) {
			Api.loadTeamMessageNotices(this, null, function (rspBody) {
				this.domainNotices = rspBody.domainNotices;
				rspBody.domainNotices.forEach(function (domainNotice) {
					var ndomain = null;
					this.domainList.forEach(function (idomain) {
						if (idomain.domainId == domainNotice.domainId) {
							ndomain = idomain;
							return false;
						}
					});
					if (ndomain) {
						ndomain.noticeCount = domainNotice.count;
					}
				}, this);
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadWhisperMessageNotices: function (onsuccess, onfailed) {
			Api.loadWhisperMessageNotices(this, null, function (rspBody) {
				this.notices = rspBody.notices;
				rspBody.notices.forEach(function (notice) {
					var nwhisper = null;
					this.whisperList.forEach(function (iwhisper) {
						if (iwhisper.whisperId == notice.whisperId) {
							nwhisper = iwhisper;
							return false;
						}
					});
					if (nwhisper) {
						nwhisper.noticeCount = notice.count;
					}
				}, this);
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserFriendNotice: function (onsuccess, onfailed) {
			Api.loadUserFriendNotice(this, null, function (rspBody) {
				this.userFriendNotice = rspBody.notice;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserMutualDomains: function (userId, onsuccess, onfailed) {
			Api.loadUserMutualDomains(this, {
				friendId: userId
			}, function (rspBody) {
				this.userInfo.mutualDomains = rspBody.domains;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserDomainList: function (onsuccess, onfailed) {
			Api.loadUserDomainList(this, null, function (rspBody) {
				this.domainList = rspBody.domainList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadTagList: function (onsuccess, onfailed) {
			Api.loadTagList(this, null, function (rspBody) {
				this.tagList = rspBody.tagList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		createDomain: function (onsuccess, onfailed) {
			Api.createDomain(this, {
				name: this.addDomain.name,
				avatar: this.addDomain.avatar,
				about: this.addDomain.about,
				address: this.addDomain.address,
				tagIds: [this.addDomain.tagId]
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.domain, rspBody.category, rspBody.team);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserFriendList: function (onsuccess, onfailed) {
			Api.loadUserFriendList(this, null, function (rspBody) {
				var users = new Array();
				rspBody.users.forEach(function (user) {
					user.addWhisperSelected = false;
					user.addWhisperUserSelected = false;
					users[user.userId] = user;
				});
				rspBody.userFriendList.forEach(function (userFriend) {
					userFriend.friend = users[userFriend.friendId];
				});
				this.userFriendList = rspBody.userFriendList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		inviteUserFriend: function (userId, onsuccess, onfailed) {
			Api.inviteUserFriend(this, {
				friendId: userId
			}, function (rspBody) {
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		cancelUserFriend: function (userId, onsuccess, onfailed) {
			Api.cancelUserFriend(this, {
				friendId: userId
			}, function (rspBody) {
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		acceptUserFriend: function (userId, onsuccess, onfailed) {
			Api.acceptUserFriend(this, {
				friendId: userId
			}, function (rspBody) {
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		rejectUserFriend: function (userId, onsuccess, onfailed) {
			Api.rejectUserFriend(this, {
				friendId: userId
			}, function (rspBody) {
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		removeUserFriend: function (userId, onsuccess, onfailed) {
			Api.removeUserFriend(this, {
				friendId: userId
			}, function (rspBody) {
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserWhisperList: function (onsuccess, onfailed) {
			Api.loadUserWhisperList(this, null, function (rspBody) {
				rspBody.whisperList.forEach(function (whisper) {
					whisper.noticeCount = 0;
				});
				this.whisperList = rspBody.whisperList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		promiseWhisper: function (member, onsuccess, onfailed) {
			Api.promiseWhisper(this, {
				member: member
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.whisper);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		createWhisper: function (members, onsuccess, onfailed) {
			Api.createWhisper(this, {
				members: members
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.whisper);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadWhisperUserList: function (whisperId, onsuccess, onfailed) {
			Api.loadWhisperUserList(this, {
				whisperId: whisperId
			}, function (rspBody) {
				var members = new Array();
				rspBody.userList.forEach(function (user) {
					members[user.userId] = user;
				});
				this.whisper.members = members;
				this.whisper.memberList = rspBody.userList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		joinWhisperUsers: function (whisperId, userIds, onsuccess, onfailed) {
			Api.joinWhisperUsers(this, {
				whisperId: whisperId,
				userIds: userIds
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.whisper);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		kickWhisperUsers: function (whisperId, userIds, onsuccess, onfailed) {
			Api.kickWhisperUsers(this, {
				whisperId: whisperId,
				userIds: userIds
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.whisper);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadWhisperMessageList: function (whisperId, fromId, toId, onsuccess, onfailed) {
			if (!whisperId) {
				return;
			}
			Api.loadWhisperMessageList(this, {
				whisperId: whisperId,
				fromId: fromId || '',
				toId: toId || '',
			}, function (rspBody) {
				var users = new Array();
				rspBody.users.forEach(function (user) {
					users[user.userId] = user;
				});
				var idomains = new Array();
				rspBody.domains.forEach(function (idomain) {
					idomains[idomain.domainId] = idomain;
				});
				var ireplyMessages = new Array();
				rspBody.replyMessages.forEach(function (ireplyMessage) {
					ireplyMessages[ireplyMessage.messageId] = ireplyMessage;
				});
				rspBody.messageList.forEach(function (message) {
					message.user = users[message.userId];
					if (message.inviteDomainId) {
						message.inviteDomain = idomains[message.inviteDomainId];
					}
					if (message.replyMessageId) {
						message.replyMessage = ireplyMessages[message.replyMessageId];
						message.replyMessage.user = users[message.replyMessage.userId];
						if (message.replyMessage.inviteDomainId) {
							message.replyMessage.inviteDomain = idomains[message.replyMessage.inviteDomainId];
						}
					}
				});
				if (fromId) {
					this.whisper.messageList = this.whisper.messageList.concat(rspBody.messageList);
				}
				else if (toId) {
					this.whisper.messageList = rspBody.messageList.concat(this.whisper.messageList);
				}
				else {
					this.whisper.messageList = rspBody.messageList;
				}
				onsuccess && onsuccess.call(this);
				
				Api.acceptWhisperMessageNotice(this, {
					whisperId: whisperId
				});
				this.notices.forEach(function (notice) {
					if (notice.whisperId == whisperId) {
						notice.count = 0;
						return false;
					}
				});
				this.whisperList.forEach(function (iwhisper) {
					if (iwhisper.whisperId == whisperId) {
						iwhisper.noticeCount = 0;
						return false;
					}
				});
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		msgListScroll: function () {
			this.$nextTick(function () {
				var msgList = this.$el.querySelector('#msg #list');
				msgList.scrollTop = msgList.scrollHeight;
			});
		}
	}
});