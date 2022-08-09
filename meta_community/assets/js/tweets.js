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
		tagList: [],
		tagId: '',
		tagIndex: 0,
		feedsTab: 'tw',//tw,rs
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
		
		this.loadUser();
		this.loadUserFriendList();
		this.loadUserDomainList();
		this.loadTagList();
		
		this.loadTeamMessageNotices();
		this.loadWhisperMessageNotices();
		this.loadUserFriendNotice();
		
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
				_this.loadUser();
				_this.loadUserFriendList();
				_this.loadUserDomainList();
				
				_this.loadTeamMessageNotices();
				_this.loadWhisperMessageNotices();
				_this.loadUserFriendNotice();
				
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
				}
				else if (data.dataType.value == 12) {
					if (!_this.userFriendNotice) {
						_this.userFriendNotice = {
							userId: _this.user.userId,
							count: 0
						};
					}
					_this.userFriendNotice.count = _this.userFriendNotice.count + 1;
				}
			};
			this.ws.oneror = function () {
				//
			};
			this.ws.onclose = function () {
				//
			};
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
		clickContainer: function () {
			if (this.activeFrame != 'addDomainDialog') {this.addDomainDialog = false;}
			if (this.activeFrame != 'userNicknameDialog') {this.userNicknameDialog = false;}
			if (this.activeFrame != 'userEmailDialog') {this.userEmailDialog = false;}
			if (this.activeFrame != 'userAvatarDialog') {this.userAvatarDialog = false;}
			if (this.activeFrame != 'userInfoDialog') {this.userInfoDialog = false;}
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
			window.location = '/home.html';
		},
		clickTweets: function () {
			//
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
		clickMe: function () {
			this.clickUserInfo(this.user.userId);
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
		clickNotificationInactiveDropdown: function () {
			this.userSettingAttrs.notification.inactiveDropdown = !this.userSettingAttrs.notification.inactiveDropdown;
			window.event.stopPropagation();
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
		clickSiteSquare: function () {
			//
		},
		clickFeedsTab: function (feedsTab) {
			this.feedsTab = feedsTab;
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
		loadUserFriendList: function (onsuccess, onfailed) {
			Api.loadUserFriendList(this, null, function (rspBody) {
				var users = new Array();
				rspBody.users.forEach(function (user) {
					user.addWhisperSelected = false;
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
		}
	}
});