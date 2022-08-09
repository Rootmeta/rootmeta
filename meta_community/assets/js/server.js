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
		whisperList: [],
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
		domainUserSetting: false,
		domain: null,
		domainOption: false,
		domainSetting: false,
		domainSettingItem: 'serverview',//serverview
		domainSettingAttrs: {
			serverview: {
				domainName: '',
				domainAddress: '',
				domainAbout: '',
				domainVisibility: false,
				domainAvatar: '',
				domainBanner: '',
				inactiveTeamDropdown: false,
				inactiveTimeDropdown: false,
				systemMessageDropdown: false,
				unsaved: false
			},
			rolesview: {
				tab: 'privilege',//display,privilege,member
				role: null,
				roleNameEdit: '',
				roleColorEdit: '',
				privilegeViewTeam: false,
				privilegeAdminTeam: false,
				privilegeAdminRole: false,
				privilegeAdminDomain: false,
				privilegeInviteMember: false,
				privilegeRemoveMember: false,
				privilegeSendMessage: false,
				searchMemberText: '',
				addRoleUserDialog: false,
				addRoleUserSearchText: '',
				unsaved: false
			},
			commview: {
				ruleTeamDropdown: false,
				updateTeamDropdown: false,
				languageDropdown: false,
				unsaved: false
			},
			members: {
				roleDropdown: false,
				selectRole: null,
				searchText: '',
				unsaved: false
			}
		},
		domainBoost: false,
		team: null,
		teamSetting: false,
		teamSettingItem: 'overview',//overview,privilege
		teamSettingAttrs: {
			overview: {
				name: '',
				about: '',
				adminOnly: 0,
				tvfLimitText: '',
				tvfTeamLimit: null,
				tokenListDropdown: false,
				tokenTeamLimitList: [],
				nftTeamLimitList: [],
				overviewUnsaved: false,
				permissionUnsaved: false
			}
		},
		domainList: [],
		categoryList: [],
		teamList: [],
		addDomainDialog: false,
		addDomain: {
			domainId: '',
			name: '',
			avatar: '',
			about: '',
			address: '',
			tagId: ''
		},
		addCategoryDialog: false,
		addCategory: {
			categoryId: '',
			name: ''
		},
		addTeamDialog: false,
		addTeam: {
			teamId: '',
			categoryId: '',
			name: '',
			about: ''
		},
		domainInviteDialog: false,
		domainInvite: {
			searchText: '',
			invitedKeys: []
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
		tvfBoostDialog: false,
		memberOption: false,
		showTalkEmoji: false,
		showReactEmoji: false,
		reactPanelTop: 0,
		reactPanelRight: 0,
		reactMessage: null,
		replyMessage: null,
		editMessage: null,
		messaging: '',
		users: new Array(),
		userList: [],
		domainRoleList: [],
		domainRoleUserList: [],
		editDomainRole: null,
		messageList: [],
		messageLoading: false,
		viewImage: false,
		viewImageUrl: '',
		viewImagePath: '',
		tagList: [],
		tokenList: [],
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
		preview: function () {
			if (!this.user || this.userList.length == 0) {
				return false;
			}
			if (this.users[this.user.userId] != null) {
				return false;
			}
			return true;
		},
		otherUserList: function () {
			var list = [];
			this.userList.forEach(function (user) {
				if (user.roleList.length==0) {
					list.push(user);
				}
			});
			return list;
		},
		editRoleUserList: function () {
			var list = [];
			if (this.domainSettingAttrs.rolesview.role) {
				this.domainSettingAttrs.rolesview.role.userList.forEach(function (roleUser) {
					if (roleUser.nickname.indexOf(this.domainSettingAttrs.rolesview.searchMemberText) >= 0) {
						list.push(roleUser);
					}
				}, this);
			}
			return list;
		},
		addRoleUserDomainUserList: function () {
			var list = [];
			this.userList.forEach(function (domainUser) {
				if (domainUser.nickname.indexOf(this.domainSettingAttrs.rolesview.addRoleUserSearchText) >= 0) {
					list.push(domainUser);
				}
			}, this);
			return list;
		},
		addRoleUserDomainUserSelectedList: function () {
			var list = [];
			this.userList.forEach(function (iuser) {
				if (iuser.addRoleUserSelected) {
					list.push(iuser);
				}
			}, this);
			return list;
		},
		searchedRoleUserList: function () {
			var list = [];
			this.userList.forEach(function (iuser) {
				if (iuser.nickname.indexOf(this.domainSettingAttrs.members.searchText) < 0) {
					return;
				}
				if (!this.domainSettingAttrs.members.selectRole || this.domainSettingAttrs.members.selectRole.isdefault.value==1) {
					list.push(iuser);
					return;
				}
				iuser.roleList.forEach(function (iuserRole) {
					if (iuserRole.roleId == this.domainSettingAttrs.members.selectRole.roleId) {
						list.push(iuser);
						return false;
					}
				}, this);
			}, this);
			return list;
		},
		//self
		userTokenBalances: function () {
			var map = new Array();
			var list = [];
			this.userTokenList.forEach(function (userToken) {
				var tokenBalance = map[userToken.tokenId];
				if (!tokenBalance) {
					tokenBalance = {
						id: userToken.token.tokenId,
						name: userToken.token.name,
						logo: userToken.token.logo,
						amount: 0
					};
					map[userToken.tokenId] = tokenBalance;
					list.push(tokenBalance);
				}
				tokenBalance.amount += userToken.amount;
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
		},
		messagePermission: function () {
			if (!this.user || !this.domain || !this.team) {
				return false;
			}
			if (this.domain.userId == this.user.userId) {
				return true;
			}
			if (!this.hasPrivilege(1)) {
				return false;
			}
			return this.teamPermission;
		},
		sendPermission: function () {
			if (!this.user || !this.domain || !this.team) {
				return false;
			}
			if (this.domain.userId == this.user.userId) {
				return true;
			}
			if (!this.team.adminOnly.value) {
				if (!this.hasPrivilege(22)) {
					return false;
				}
			}
			else {
				if (!this.hasPrivilege(8)) {
					return false;
				}
			}
			return this.teamPermission;
		},
		teamPermission: function () {
			if (!this.user || !this.domain || !this.team) {
				return true;
			}
			if (this.domain.userId == this.user.userId) {
				return true;
			}
			if (this.teamSettingAttrs.overview.tvfTeamLimit && this.teamSettingAttrs.overview.tvfTeamLimit.tvfLimit > this.user.deAmount) {
				return false;
			}
			for (var i = 0; i < this.teamSettingAttrs.overview.tokenTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.tokenTeamLimitList[i];
				var permit = false;
				for (var j = 0; j < this.userTokenBalances.length; j++) {
					var ibalance = this.userTokenBalances[j];
					if (ibalance.id == ilimit.tokenId && ibalance.amount >= ilimit.tokenLimit) {
						permit = true;
						break;
					}
				}
				if (!permit) {
					return false;
				}
			}
			for (var i = 0; i < this.teamSettingAttrs.overview.nftTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.nftTeamLimitList[i];
				var permit = false;
				for (var j = 0; j < this.userNftList.length; j++) {
					var inft = this.userNftList[j];
					if (inft.contract == ilimit.nftContract) {
						permit = true;
						break;
					}
				}
				if (!permit) {
					return false;
				}
			}
			return true;
		},
		teamPermissionLimitList: function () {
			var list = [];
			if (this.teamSettingAttrs.overview.tvfTeamLimit) {
				this.teamSettingAttrs.overview.tvfTeamLimit.permit = (this.user && this.user.deAmount >= this.teamSettingAttrs.overview.tvfTeamLimit.tvfLimit);
				list.push(this.teamSettingAttrs.overview.tvfTeamLimit);
			}
			for (var i = 0; i < this.teamSettingAttrs.overview.tokenTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.tokenTeamLimitList[i];
				var permit = false;
				for (var j = 0; j < this.userTokenBalances.length; j++) {
					var ibalance = this.userTokenBalances[j];
					if (ibalance.id == ilimit.tokenId && ibalance.amount >= ilimit.tokenLimit) {
						permit = true;
						break;
					}
				}
				ilimit.permit = permit;
				list.push(ilimit);
			}
			for (var i = 0; i < this.teamSettingAttrs.overview.nftTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.nftTeamLimitList[i];
				var permit = false;
				for (var j = 0; j < this.userNftList.length; j++) {
					var inft = this.userNftList[j];
					if (inft.contract == ilimit.nftContract) {
						permit = true;
						break;
					}
				}
				ilimit.permit = permit;
				list.push(ilimit);
			}
			return list;
		},
		invitableList: function () {
			var list = [];
			this.userFriendList.forEach(function (userFriend) {
				if (userFriend.friendStatus.value == 4 && userFriend.friend.nickname.indexOf(this.domainInvite.searchText) >= 0) {
					var key = 'friend-' + userFriend.friend.userId;
					var invited = false;
					this.domainInvite.invitedKeys.forEach(function (item) {
						if (item == key) {
							invited = true;
							return false;
						}
					});
					list.push({
						type: 'friend',
						key: key,
						id: userFriend.friend.userId,
						name: userFriend.friend.nickname,
						avatar: userFriend.friend.avatar || '/assets/img/user_avatar.png',
						invited: invited
					});
				}
			}, this);
			this.whisperList.forEach(function (whisper) {
				if (!whisper.memberId && whisper.name.indexOf(this.domainInvite.searchText) >= 0) {
					var key = 'whisper-' + whisper.whisperId;
					var invited = false;
					this.domainInvite.invitedKeys.forEach(function (item) {
						if (item == key) {
							invited = true;
							return false;
						}
					});
					list.push({
						type: 'whisper',
						key: key,
						id: whisper.whisperId,
						name: whisper.name,
						avatar: '/assets/img/chat_group_g.png',
						invited: invited
					});
				}
			}, this);
			this.domainList.forEach(function (domain) {
				if (this.domain && domain.domainId != this.domain.domainId && domain.name.indexOf(this.domainInvite.searchText) >= 0) {
					var key = 'domain-' + domain.domainId;
					var invited = false;
					this.domainInvite.invitedKeys.forEach(function (item) {
						if (item == key) {
							invited = true;
							return false;
						}
					});
					list.push({
						type: 'domain',
						key: key,
						id: domain.domainId,
						name: domain.name,
						avatar: domain.avatar,
						invited: invited
					});
				}
			}, this);
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
		
		this.connect();
		
		this.loadUser();
		this.loadUserFriendList();
		this.loadUserWhisperList();
		this.loadUserDomainList(function () {
			var domainId = null;
			if (window.location.hash) {
				domainId = Url.decode(Url.hash('id'));
			}
			else {
				var path = window.location.pathname;
				var offset = '/server/'.length;
				if (path.length > offset) {
					domainId = Url.decode(path.substring(offset));
				}
			}
			if (!domainId && this.domainList.length > 0) {
				domainId = this.domainList[0].domainId;
			}
			if (domainId) {
				this.loadDomain(domainId, function () {
					if (this.domain) {
						this.loadDomainTeamList(domainId, function () {
							if (this.teamList.length) {
								this.team = this.teamList[0];
								this.teamSettingAttrs.overview.name = this.team.name;
								this.teamSettingAttrs.overview.about = this.team.about;
								this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
								this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
									this.msgListScroll();
									
									this.loadTeamMessageNotices();
								});
								this.loadTeamLimitListByTeam(this.team.teamId);
							}
							else {
								this.team = null;
								this.teamSettingAttrs.overview.name = '';
								this.teamSettingAttrs.overview.about = '';
								this.teamSettingAttrs.overview.adminOnly = 0;
								this.teamSettingAttrs.overview.tvfLimitText = '';
								this.teamSettingAttrs.overview.tvfTeamLimit = null;
								this.teamSettingAttrs.overview.tokenTeamLimitList = [];
								this.teamSettingAttrs.overview.nftTeamLimitList = [];
								this.messageList = [];
								
								this.loadTeamMessageNotices();
							}
						});
						this.loadDomainUserList(domainId);
					}
					else {
						this.categoryList = [];
						this.teamList = [];
						this.team = null;
						this.teamSettingAttrs.overview.name = '';
						this.teamSettingAttrs.overview.about = '';
						this.teamSettingAttrs.overview.adminOnly = 0;
						this.teamSettingAttrs.overview.tvfLimitText = '';
						this.teamSettingAttrs.overview.tvfTeamLimit = null;
						this.teamSettingAttrs.overview.tokenTeamLimitList = [];
						this.teamSettingAttrs.overview.nftTeamLimitList = [];
						this.users = new Array();
						this.userList = [];
						this.domainRoleList = [];
						this.domainRoleUserList = [];
						this.messageList = [];
						
						this.loadTeamMessageNotices();
					}
				});
			}
			else {
				this.domain = null;
				this.domainList = [];
				this.categoryList = [];
				this.teamList = [];
				this.team = null;
				this.teamSettingAttrs.overview.name = '';
				this.teamSettingAttrs.overview.about = '';
				this.teamSettingAttrs.overview.adminOnly = 0;
				this.teamSettingAttrs.overview.tvfLimitText = '';
				this.teamSettingAttrs.overview.tvfTeamLimit = null;
				this.teamSettingAttrs.overview.tokenTeamLimitList = [];
				this.teamSettingAttrs.overview.nftTeamLimitList = [];
				this.users = new Array();
				this.userList = [];
				this.domainRoleList = [];
				this.domainRoleUserList = [];
				this.messageList = [];
				
				this.loadTeamMessageNotices();
			}
			
			this.loadWhisperMessageNotices();
			this.loadUserFriendNotice();
		});
		this.loadTagList();
		this.loadTokenList();
		
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
				
				_this.connect();
				
				//reinit
				_this.loadUser();
				_this.loadUserFriendList();
				_this.loadUserWhisperList();
				_this.loadUserDomainList(function () {
					Url.hash('id', '');
					var domainId = Url.decode(Url.hash('id'));
					if (!domainId && _this.domainList.length > 0) {
						domainId = _this.domainList[0].domainId;
					}
					if (domainId) {
						window.location = '/server/' + Url.encode(domainId);
						return;
						_this.loadDomain(domainId, function () {
							if (_this.domain) {
								_this.loadDomainTeamList(domainId, function () {
									if (_this.teamList.length) {
										_this.team = _this.teamList[0];
										_this.teamSettingAttrs.overview.name = _this.team.name;
										_this.teamSettingAttrs.overview.about = _this.team.about;
										_this.teamSettingAttrs.overview.adminOnly = _this.team.adminOnly.value;
										_this.loadTeamMessageList(_this.team.domainId, _this.team.teamId, null, null, function () {
											_this.msgListScroll();
											
											_this.loadTeamMessageNotices();
											_this.loadWhisperMessageNotices();
											_this.loadUserFriendNotice();
										});
										_this.loadTeamLimitListByTeam(_this.team.teamId);
									}
									else {
										_this.team = null;
										_this.teamSettingAttrs.overview.name = '';
										_this.teamSettingAttrs.overview.about = '';
										_this.teamSettingAttrs.overview.adminOnly = 0;
										_this.teamSettingAttrs.overview.tvfLimitText = '';
										_this.teamSettingAttrs.overview.tvfTeamLimit = null;
										_this.teamSettingAttrs.overview.tokenTeamLimitList = [];
										_this.teamSettingAttrs.overview.nftTeamLimitList = [];
										_this.messageList = [];
										
										_this.loadTeamMessageNotices();
										_this.loadWhisperMessageNotices();
										_this.loadUserFriendNotice();
									}
								});
								_this.loadDomainUserList(domainId);
							}
							else {
								_this.categoryList = [];
								_this.teamList = [];
								_this.team = null;
								_this.teamSettingAttrs.overview.name = '';
								_this.teamSettingAttrs.overview.about = '';
								_this.teamSettingAttrs.overview.adminOnly = 0;
								_this.teamSettingAttrs.overview.tvfLimitText = '';
								_this.teamSettingAttrs.overview.tvfTeamLimit = null;
								_this.teamSettingAttrs.overview.tokenTeamLimitList = [];
								_this.teamSettingAttrs.overview.nftTeamLimitList = [];
								_this.users = new Array();
								_this.userList = [];
								_this.domainRoleList = [];
								_this.domainRoleUserList = [];
								_this.messageList = [];
								
								_this.loadTeamMessageNotices();
								_this.loadWhisperMessageNotices();
								_this.loadUserFriendNotice();
							}
						});
					}
					else {
						window.location = '/home.html';
						return;
						_this.domain = null;
						_this.domainList = [];
						_this.categoryList = [];
						_this.teamList = [];
						_this.team = null;
						_this.teamSettingAttrs.overview.name = '';
						_this.teamSettingAttrs.overview.about = '';
						_this.teamSettingAttrs.overview.adminOnly = 0;
						_this.teamSettingAttrs.overview.tvfLimitText = '';
						_this.teamSettingAttrs.overview.tvfTeamLimit = null;
						_this.teamSettingAttrs.overview.tokenTeamLimitList = [];
						_this.teamSettingAttrs.overview.nftTeamLimitList = [];
						_this.users = new Array();
						_this.userList = [];
						_this.domainRoleList = [];
						_this.domainRoleUserList = [];
						_this.messageList = [];
						
						_this.loadTeamMessageNotices();
						_this.loadWhisperMessageNotices();
						_this.loadUserFriendNotice();
					}
				});
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
					if (message.teamId == _this.team.teamId) {
						Api.loadTeamMessage(_this, {
							messageId: message.messageId
						}, function (rspBody) {
							var msg = rspBody.message;
							_this.messageList.push(msg);
							_this.msgListScroll();
							Api.acceptTeamMessageNotice(_this, {
								teamId: message.teamId
							});
						}, function (code, message) {
							MsgBox.alert(message);
						});
					}
					else {
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
						_this.teamList.forEach(function (iteam) {
							if (iteam.teamId == message.teamId) {
								iteam.noticeCount = iteam.noticeCount + 1;
								return false;
							}
						});
					}
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
		yesMsgBox: function (e) {
			e.stopPropagation();
			MsgBox.yes();
		},
		noMsgBox: function (e) {
			e.stopPropagation();
			MsgBox.no();
		},
		clickContainer: function () {
			if (this.activeFrame != 'domainOption') {this.domainOption = false;}
			if (this.activeFrame != 'domainBoost') {this.domainBoost = false;}
			if (this.activeFrame != 'teamSetting') {this.teamSetting = false;}
			if (this.activeFrame != 'addDomainDialog') {this.addDomainDialog = false;}
			if (this.activeFrame != 'addCategoryDialog') {this.addCategoryDialog = false;}
			if (this.activeFrame != 'addTeamDialog') {this.addTeamDialog = false;}
			if (this.activeFrame != 'domainInviteDialog') {this.domainInviteDialog = false;}
			if (this.activeFrame != 'userInfoDialog') {this.userInfoDialog = false;}
			if (this.activeFrame != 'userNicknameDialog') {this.userNicknameDialog = false;}
			if (this.activeFrame != 'userEmailDialog') {this.userEmailDialog = false;}
			if (this.activeFrame != 'userAvatarDialog') {this.userAvatarDialog = false;}
			if (this.activeFrame != 'memberOption') {this.memberOption = false;}
			if (this.activeFrame != 'showTalkEmoji') {this.showTalkEmoji = false;}
			if (this.activeFrame != 'showReactEmoji') {this.showReactEmoji = false;}
			if (this.activeFrame != 'addRoleUserDialog') {this.domainSettingAttrs.rolesview.addRoleUserDialog = false;}
			if (this.activeFrame != 'tvfBoostDialog') {this.tvfBoostDialog = false;}
			this.teamSettingAttrs.overview.tokenListDropdown = false;
			this.userSettingAttrs.notification.inactiveDropdown = false;
			this.domainSettingAttrs.serverview.inactiveTeamDropdown = false;
			this.domainSettingAttrs.serverview.inactiveTimeDropdown = false;
			this.domainSettingAttrs.serverview.systemMessageDropdown = false;
			this.domainSettingAttrs.commview.ruleTeamDropdown = false;
			this.domainSettingAttrs.commview.updateTeamDropdown = false;
			this.domainSettingAttrs.commview.languageDropdown = false;
			this.domainSettingAttrs.members.roleDropdown = false;
			this.activeFrame = null;
		},
		clickLanguage: function (locale) {
			this.$i18n.locale = locale;
			Store.set('locale', locale);
		},
		clickShowFrame: function (frame) {
			this.activeFrame = frame;
		},
		hasPrivilege: function (privilege) {
			if (!this.user || !this.domain) {
				return false;
			}
			if (this.domain.userId == this.user.userId) {
				return true;
			}
			for (var i = 0; i < this.domain.privileges.length; i++) {
				if (this.domain.privileges[i] == privilege) {
					return true;
				}
			}
			return false;
		},
		clickHome: function () {
			window.location = '/home.html';
		},
		clickTweets: function () {
			window.location = '/tweets.html';
		},
		clickExploreDomain: function () {
			window.location = '/explore.html';
		},
		clickExitPreviewDomain: function () {
			window.history.back(1);
		},
		clickJoinDomain: function () {
			if (!this.domain) {
				return;
			}
			
			this.joinDomain(this.domain.domainId, function () {
				this.loadUserDomainList(function () {
					var domainId = this.domain.domainId;
					this.loadDomain(domainId, function () {
						this.loadDomainTeamList(domainId, function () {
							if (this.teamList.length) {
								this.team = this.teamList[0];
								this.teamSettingAttrs.overview.name = this.team.name;
								this.teamSettingAttrs.overview.about = this.team.about;
								this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
								this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
									this.msgListScroll();
								});
								this.loadTeamLimitListByTeam(this.team.teamId);
							}
						});
						this.loadDomainUserList(domainId);
					});
				});
			});
		},
		clickDomain: function (domainId) {
			window.location = '/server/' + Url.encode(domainId);
			return;
			Url.hash('id', Url.encode(domainId));
			this.loadDomain(domainId, function () {
				this.loadDomainTeamList(domainId, function () {
					if (this.teamList.length) {
						this.team = this.teamList[0];
						this.teamSettingAttrs.overview.name = this.team.name;
						this.teamSettingAttrs.overview.about = this.team.about;
						this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
						this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
							this.msgListScroll();
							
							this.domainNotices.forEach(function (domainNotice) {
								if (domainNotice.domainId == domainId) {
									domainNotice.teamNotices.forEach(function (teamNotice) {
										var nteam = null;
										this.teamList.forEach(function (iteam) {
											if (iteam.teamId == teamNotice.teamId) {
												nteam = iteam;
												return false;
											}
										});
										if (nteam) {
											nteam.noticeCount = teamNotice.count;
										}
									}, this);
									return false;
								}
							}, this);
						});
						this.loadTeamLimitListByTeam(this.team.teamId);
					}
				});
				this.loadDomainUserList(domainId);
			});
		},
		clickTeam: function (teamId) {
			if (this.team && this.team.teamId == teamId) {
				return;
			}
			this.teamList.forEach(function (team) {
				if (team.teamId == teamId) {
					this.team = team;
					this.teamSettingAttrs.overview.name = this.team.name;
					this.teamSettingAttrs.overview.about = this.team.about;
					this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
					return false;
				}
			}, this);
			this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
				this.msgListScroll();
			});
			this.loadTeamLimitListByTeam(teamId);
		},
		clickDomainHead: function () {
			if (this.preview) {
				return;
			}
			this.domainOption = !this.domainOption;
			window.event.stopPropagation();
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
				this.addDomainDialog = false;
				window.location = '/server/' + Url.encode(domain.domainId);
				return;
				this.loadUserDomainList(function () {
					var domainId = domain.domainId;
					this.loadDomain(domainId, function () {
						Url.hash('id', Url.encode(domainId));
						this.loadDomainTeamList(domainId, function () {
							if (this.teamList.length) {
								this.team = this.teamList[0];
								this.teamSettingAttrs.overview.name = this.team.name;
								this.teamSettingAttrs.overview.about = this.team.about;
								this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
								this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
									this.msgListScroll();
								});
								this.loadTeamLimitListByTeam(this.team.teamId);
							}
						});
						this.loadDomainUserList(domainId);
					});
				});
			});
		},
		clickAddDomainCancel: function () {
			this.addDomainDialog = false;
		},
		clickAddDomainTag: function (tagId) {
			this.addDomain.tagId = tagId;
		},
		clickAddCategory: function () {
			if (this.preview) {
				return;
			}
			if (!this.domain) {
				//alert('Create Island First.');
				return;
			}
			this.addCategory.categoryId = '';
			this.addCategory.name = '';
			this.addCategoryDialog = true;
			window.event.stopPropagation();
		},
		clickAddCategoryConfirm: function () {
			this.createCategory(function (category) {
				this.addCategoryDialog = false;
				var domainId = this.domain.domainId;
				this.loadDomainTeamList(domainId);
			});
		},
		clickAddCategoryCancel: function () {
			this.addCategoryDialog = false;
		},
		clickAddTeam: function (categoryId) {
			if (this.preview) {
				return;
			}
			if (!this.domain) {
				//alert('Create Island First.');
				return;
			}
			this.addTeam.teamId = '';
			this.addTeam.categoryId = categoryId || '';
			this.addTeam.name = '';
			this.addTeam.about = '';
			this.addTeamDialog = true;
			window.event.stopPropagation();
		},
		clickAddTeamConfirm: function () {
			this.createTeam(function (team) {
				this.addTeamDialog = false;
				var domainId = this.domain.domainId;
				this.loadDomainTeamList(domainId, function () {
					this.team = team;
					this.teamSettingAttrs.overview.name = this.team.name;
					this.teamSettingAttrs.overview.about = this.team.about;
					this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
					this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
						this.msgListScroll();
					});
					this.loadTeamLimitListByTeam(this.team.teamId);
				});
			});
		},
		clickAddTeamCancel: function () {
			this.addTeamDialog = false;
		},
		clickDomainInvite: function () {
			this.domainInviteDialog = true;
		},
		clickDomainInviteInvite: function (type, id, key) {
			if (type == 'friend') {
				Api.inviteDomainByFriend(this, {
					friendId: id,
					inviteDomainId: this.domain.domainId
				}, function (rspBody) {
					this.domainInvite.invitedKeys.push(key);
				}, function (code, message) {
					MsgBox.alert(message);
				});
			}
			else if (type == 'whisper') {
				Api.inviteDomainByWhisper(this, {
					whisperId: id,
					inviteDomainId: this.domain.domainId
				}, function (rspBody) {
					this.domainInvite.invitedKeys.push(key);
				}, function (code, message) {
					MsgBox.alert(message);
				});
			}
			else if (type == 'domain') {
				Api.inviteDomainByDomain(this, {
					domainId: id,
					inviteDomainId: this.domain.domainId
				}, function (rspBody) {
					this.domainInvite.invitedKeys.push(key);
				}, function (code, message) {
					MsgBox.alert(message);
				});
			}
		},
		clickDomainInviteCancel: function () {
			this.domainInviteDialog = false;
		},
		clickInviteTeamUser: function (teamId) {
			window.event.stopPropagation();
		},
		clickMe: function () {
			this.clickUserInfo(this.user.userId);
		},
		clickTvfBoost: function () {
			this.tvfBoostDialog = true;
			window.event.stopPropagation();
		},
		clickTvfBoostConfirm: function () {
			Api.boostDomain(this, {
				domainId: this.domain.domainId
			}, function (rspBody) {
				this.tvfBoostDialog = false;
				MsgBox.alert(this.$t('server.power.tvfBoostOk'));
				this.loadDomain(this.domain.domainId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickTvfBoostCancel: function () {
			this.tvfBoostDialog = false;
		},
		clickBuildDomainPoster: function () {
			Api.buildDomainPoster(this, {
				domainId: this.domain.domainId
			}, function (rspBody) {
				this.viewImageUrl = rspBody.url;
				this.viewImagePath = rspBody.path;
				this.viewImage = true;
			}, function (code, message) {
				MsgBox.alert(message);
			});
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
		clickMember: function (memberId) {
			this.clickUserInfo(memberId);
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
				window.location = '/home.html?wid=' + Url.encode(whisper.whisperId);
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
		clickDomainSetting: function () {
			this.domainSetting = true;
			window.event.stopPropagation();
		},
		clickDomainSettingItem: function (domainSettingItem) {
			if (domainSettingItem != 'delete') {
				this.domainSettingItem = domainSettingItem;
				return;
			}
			var _this = this;
			MsgBox.confirm(_this.$t('domainSetting.deleteDomainConfirm'), function () {
				Api.deleteDomain(_this, {
					domainId: _this.domain.domainId
				}, function (rspBody) {
					_this.domainSetting = false;
					for (var i = 0; i < _this.domainList.length; i++) {
						var idomain = _this.domainList[i];
						if (idomain.domainId == _this.domain.domainId) {
							_this.domainList.splice(i, 1);
							break;
						}
					}
					if (_this.domainList.length) {
						_this.clickDomain(_this.domainList[0].domainId);
					}
					else {
						_this.domain = null;
						_this.domainList = [];
						_this.categoryList = [];
						_this.teamList = [];
						_this.team = null;
						_this.teamSettingAttrs.overview.name = '';
						_this.teamSettingAttrs.overview.about = '';
						_this.teamSettingAttrs.overview.adminOnly = 0;
						_this.teamSettingAttrs.overview.tvfLimitText = '';
						_this.teamSettingAttrs.overview.tvfTeamLimit = null;
						_this.teamSettingAttrs.overview.tokenTeamLimitList = [];
						_this.teamSettingAttrs.overview.nftTeamLimitList = [];
						_this.users = new Array();
						_this.userList = [];
						_this.domainRoleList = [];
						_this.domainRoleUserList = [];
						_this.messageList = [];
						
						_this.loadTeamMessageNotices();
					}
				}, function (code, message) {
					MsgBox.alert(message);
				});
			});
		},
		inputDomainSettingDomainName: function () {
			this.domainSettingAttrs.serverview.unsaved = true;
		},
		inputDomainSettingDomainAddress: function () {
			this.domainSettingAttrs.serverview.unsaved = true;
		},
		inputDomainSettingDomainAbout: function () {
			this.domainSettingAttrs.serverview.unsaved = true;
		},
		clickDomainVisibility: function () {
			this.domainSettingAttrs.serverview.unsaved = true;
		},
		changeDomainSettingDomainAvatar: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				this.domainSettingAttrs.serverview.domainAvatar = result.url;
			}, function (message) {
				MsgBox.alert(message);
			});
			this.domainSettingAttrs.serverview.unsaved = true;
		},
		changeDomainSettingDomainBanner: function (data) {
			Api.upload(this, data.target.files[0], function (result) {
				this.domainSettingAttrs.serverview.domainBanner = result.url;
			}, function (message) {
				MsgBox.alert(message);
			});
			this.domainSettingAttrs.serverview.unsaved = true;
		},
		clickDomainSettingSaveOverview: function () {
			Api.editDomain(this, {
				domainId: this.domain.domainId,
				name: this.domainSettingAttrs.serverview.domainName,
				address: this.domainSettingAttrs.serverview.domainAddress,
				about: this.domainSettingAttrs.serverview.domainAbout,
				avatar: this.domainSettingAttrs.serverview.domainAvatar,
				banner: this.domainSettingAttrs.serverview.domainBanner,
				visibility: (this.domainSettingAttrs.serverview.domainVisibility ? 2 : 1)
			}, function (rspBody) {
				this.domain = rspBody.domain;
				this.domainSettingAttrs.serverview.domainName = this.domain.name;
				this.domainSettingAttrs.serverview.domainAddress = this.domain.address;
				this.domainSettingAttrs.serverview.domainAbout = this.domain.about;
				this.domainSettingAttrs.serverview.domainAvatar = this.domain.avatar;
				this.domainSettingAttrs.serverview.domainBanner = this.domain.banner;
				this.domainSettingAttrs.serverview.domainVisibility = (this.domain.visibility.value == 2);
				this.domainSettingAttrs.serverview.unsaved = false;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickDomainSettingResetOverview: function () {
			this.domainSettingAttrs.serverview.domainName = this.domain.name;
			this.domainSettingAttrs.serverview.domainAddress = this.domain.address;
			this.domainSettingAttrs.serverview.domainAbout = this.domain.about;
			this.domainSettingAttrs.serverview.domainAvatar = this.domain.avatar;
			this.domainSettingAttrs.serverview.domainBanner = this.domain.banner;
			this.domainSettingAttrs.serverview.domainVisibility = (this.domain.visibility.value == 2);
			this.domainSettingAttrs.serverview.unsaved = false;
		},
		clickDomainRole: function (roleId) {
			this.domainRoleList.forEach(function (domainRole) {
				if (domainRole.roleId == roleId) {
					if (domainRole.isdefault.value) {
						this.domainSettingAttrs.rolesview.tab = 'privilege';
					}
					this.domainSettingAttrs.rolesview.role = domainRole;
					this.domainSettingAttrs.rolesview.roleNameEdit = domainRole.name;
					this.domainSettingAttrs.rolesview.roleColorEdit = domainRole.color;
					this.domainSettingAttrs.rolesview.privilegeViewTeam = false;
					this.domainSettingAttrs.rolesview.privilegeAdminTeam = false;
					this.domainSettingAttrs.rolesview.privilegeAdminRole = false;
					this.domainSettingAttrs.rolesview.privilegeAdminDomain = false;
					this.domainSettingAttrs.rolesview.privilegeInviteMember = false;
					this.domainSettingAttrs.rolesview.privilegeRemoveMember = false;
					this.domainSettingAttrs.rolesview.privilegeSendMessage = false;
					domainRole.privileges.forEach(function (privilege) {
						if (privilege == 1) {
							this.domainSettingAttrs.rolesview.privilegeViewTeam = true;
						}
						else if (privilege == 2) {
							this.domainSettingAttrs.rolesview.privilegeAdminTeam = true;
						}
						else if (privilege == 4) {
							this.domainSettingAttrs.rolesview.privilegeAdminRole = true;
						}
						else if (privilege == 8) {
							this.domainSettingAttrs.rolesview.privilegeAdminDomain = true;
						}
						else if (privilege == 12) {
							this.domainSettingAttrs.rolesview.privilegeInviteMember = true;
						}
						else if (privilege == 14) {
							this.domainSettingAttrs.rolesview.privilegeRemoveMember = true;
						}
						else if (privilege == 22) {
							this.domainSettingAttrs.rolesview.privilegeSendMessage = true;
						}
					}, this);
					return false;
				}
			}, this);
		},
		clickDomainRoleAdd: function () {
			Api.saveDomainRole(this, {
				roleId: '',
				domainId: this.domain.domainId
			}, function (rspBody) {
				rspBody.domainRole.userList = [];
				this.domainRoleList.push(rspBody.domainRole);
				this.clickDomainRole(rspBody.domainRole.roleId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickDomainTab: function (tab) {
			if (this.domainSettingAttrs.rolesview.role && this.domainSettingAttrs.rolesview.role.isdefault.value) {
				if (tab == 'display' || tab == 'member') {
					return;
				}
			}
			this.domainSettingAttrs.rolesview.tab = tab;
		},
		inputDomainRoleName: function () {
			this.domainSettingAttrs.rolesview.unsaved = true;
		},
		clickChangeRoleColor: function (roleColor) {
			this.domainSettingAttrs.rolesview.roleColorEdit = roleColor;
			this.domainSettingAttrs.rolesview.unsaved = true;
		},
		clickDomainRolePrivilege: function (privilege) {
			this.domainSettingAttrs.rolesview.unsaved = true;
		},
		clickDomainRoleAddMember: function () {
			this.domainSettingAttrs.rolesview.addRoleUserSearchText = '';
			this.userList.forEach(function (iuser) {
				iuser.addRoleUserSelected = false;
			});
			this.domainSettingAttrs.rolesview.addRoleUserDialog = true;
			window.event.stopPropagation();
		},
		clickDomainRoleAddMemberConfirm: function () {
			var userIds = [];
			this.userList.forEach(function (iuser) {
				if (iuser.addRoleUserSelected) {
					userIds.push(iuser.userId);
				}
			});
			Api.addDomainRoleUser(this, {
				domainId: this.domain.domainId,
				roleId: this.domainSettingAttrs.rolesview.role.roleId,
				userIds: userIds
			}, function (rspBody) {
				this.domainSettingAttrs.rolesview.addRoleUserDialog = false;
				this.loadDomainUserList(this.domain.domainId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickDomainRoleAddMemberCancel: function () {
			this.domainSettingAttrs.rolesview.addRoleUserDialog = false;
		},
		clickDomainRoleAddMemberAdd: function (userId) {
			this.userList.forEach(function (iuser) {
				if (iuser.userId == userId) {
					iuser.addRoleUserSelected = true;
					return false;
				}
			});
		},
		clickDomainRoleAddMemberRemove: function (userId) {
			this.userList.forEach(function (iuser) {
				if (iuser.userId == userId) {
					iuser.addRoleUserSelected = false;
					return false;
				}
			});
		},
		clickRemoveDomainRoleUser: function (userId) {
			Api.removeDomainRoleUser(this, {
				domainId: this.domain.domainId,
				roleId: this.domainSettingAttrs.rolesview.role.roleId,
				userIds: [userId]
			}, function (rspBody) {
				this.loadDomainUserList(this.domain.domainId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickDomainSettingSaveRolesview: function () {
			var privileges = [];
			if (this.domainSettingAttrs.rolesview.privilegeViewTeam) {
				privileges.push(1);
			}
			if (this.domainSettingAttrs.rolesview.privilegeAdminTeam) {
				privileges.push(2);
			}
			if (this.domainSettingAttrs.rolesview.privilegeAdminRole) {
				privileges.push(4);
			}
			if (this.domainSettingAttrs.rolesview.privilegeAdminDomain) {
				privileges.push(8);
			}
			if (this.domainSettingAttrs.rolesview.privilegeInviteMember) {
				privileges.push(12);
			}
			if (this.domainSettingAttrs.rolesview.privilegeRemoveMember) {
				privileges.push(14);
			}
			if (this.domainSettingAttrs.rolesview.privilegeSendMessage) {
				privileges.push(22);
			}
			Api.saveDomainRole(this, {
				roleId: this.domainSettingAttrs.rolesview.role.roleId,
				domainId: this.domain.domainId,
				name: this.domainSettingAttrs.rolesview.roleNameEdit,
				color: this.domainSettingAttrs.rolesview.roleColorEdit,
				privileges: privileges
			}, function (rspBody) {
				this.domainSettingAttrs.rolesview.role.name = this.domainSettingAttrs.rolesview.roleNameEdit;
				this.domainSettingAttrs.rolesview.role.color = this.domainSettingAttrs.rolesview.roleColorEdit;
				this.domainSettingAttrs.rolesview.role.privileges = privileges;
				this.domainSettingAttrs.rolesview.unsaved = false;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickDomainSettingResetRolesview: function () {
			this.domainSettingAttrs.rolesview.roleNameEdit = this.domainSettingAttrs.rolesview.role.name;
			this.domainSettingAttrs.rolesview.roleColorEdit = this.domainSettingAttrs.rolesview.role.color;
			this.domainSettingAttrs.rolesview.privilegeViewTeam = false;
			this.domainSettingAttrs.rolesview.privilegeAdminTeam = false;
			this.domainSettingAttrs.rolesview.privilegeAdminRole = false;
			this.domainSettingAttrs.rolesview.privilegeAdminDomain = false;
			this.domainSettingAttrs.rolesview.privilegeInviteMember = false;
			this.domainSettingAttrs.rolesview.privilegeRemoveMember = false;
			this.domainSettingAttrs.rolesview.privilegeSendMessage = false;
			this.domainSettingAttrs.rolesview.role.privileges.forEach(function (privilege) {
				if (privilege == 1) {
					this.domainSettingAttrs.rolesview.privilegeViewTeam = true;
				}
				else if (privilege == 2) {
					this.domainSettingAttrs.rolesview.privilegeAdminTeam = true;
				}
				else if (privilege == 4) {
					this.domainSettingAttrs.rolesview.privilegeAdminRole = true;
				}
				else if (privilege == 8) {
					this.domainSettingAttrs.rolesview.privilegeAdminDomain = true;
				}
				else if (privilege == 12) {
					this.domainSettingAttrs.rolesview.privilegeInviteMember = true;
				}
				else if (privilege == 14) {
					this.domainSettingAttrs.rolesview.privilegeRemoveMember = true;
				}
				else if (privilege == 22) {
					this.domainSettingAttrs.rolesview.privilegeSendMessage = true;
				}
			}, this);
			this.domainSettingAttrs.rolesview.unsaved = false;
		},
		clickDomainSettingCancel: function () {
			this.domainSetting = false;
		},
		clickDomainBoost: function () {
			this.domainBoost = true;
			window.event.stopPropagation();
		},
		clickDomainBoostCancel: function () {
			this.domainBoost = false;
		},
		clickTeamSetting: function (teamId) {
			window.event.stopPropagation();
			if (this.team && this.team.teamId == teamId) {
				this.teamSetting = true;
				return;
			}
			this.teamList.forEach(function (team) {
				if (team.teamId == teamId) {
					this.team = team;
					this.teamSettingAttrs.overview.name = this.team.name;
					this.teamSettingAttrs.overview.about = this.team.about;
					this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
					return false;
				}
			}, this);
			this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
				this.msgListScroll();
			});
			this.loadTeamLimitListByTeam(teamId, function () {
				this.teamSetting = true;
			});
		},
		clickTeamSettingItem: function (teamSettingItem) {
			if (teamSettingItem != 'delete') {
				this.teamSettingItem = teamSettingItem;
				return;
			}
			var _this = this;
			MsgBox.confirm(_this.$t('teamSetting.delTeamConfirm'), function () {
				Api.deleteTeam(_this, {
					teamId: _this.team.teamId
				}, function (rspBody) {
					_this.teamSetting = false;
					for (var i = 0; i < _this.teamList.length; i++) {
						var iteam = _this.teamList[i];
						if (iteam.teamId == _this.team.teamId) {
							_this.teamList.splice(i, 1);
							break;
						}
					}
					if (_this.teamList.length) {
						_this.clickTeam(_this.teamList[0].teamId);
					}
					else {
						_this.team = null;
						_this.teamSettingAttrs.overview.name = '';
						_this.teamSettingAttrs.overview.about = '';
						_this.teamSettingAttrs.overview.adminOnly = 0;
						_this.teamSettingAttrs.overview.tvfLimitText = '';
						_this.teamSettingAttrs.overview.tvfTeamLimit = null;
						_this.teamSettingAttrs.overview.tokenTeamLimitList = [];
						_this.teamSettingAttrs.overview.nftTeamLimitList = [];
						_this.messageList = [];
						
						_this.loadTeamMessageNotices();
					}
				}, function (code, message) {
					MsgBox.alert(message);
				});
			});
		},
		inputTeamSettingOverviewName: function () {
			this.teamSettingAttrs.overview.overviewUnsaved = true;
		},
		inputTeamSettingOverviewAbout: function () {
			this.teamSettingAttrs.overview.overviewUnsaved = true;
		},
		switchTeamSettingOverviewAdminOnly: function () {
			this.teamSettingAttrs.overview.overviewUnsaved = true;
		},
		clickTeamSettingSaveOverview: function () {
			Api.editTeam(this, {
				teamId: this.team.teamId,
				name: this.teamSettingAttrs.overview.name,
				about: this.teamSettingAttrs.overview.about,
				adminOnly: this.teamSettingAttrs.overview.adminOnly ? 1 : 0
			}, function (rspBody) {
				this.teamSettingAttrs.overview.overviewUnsaved = false;
				this.team.name = rspBody.team.name;
				this.team.about = rspBody.team.about;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickTeamSettingResetOverview: function () {
			this.teamSettingAttrs.overview.name = this.team.name;
			this.teamSettingAttrs.overview.about = this.team.about;
			this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
			this.teamSettingAttrs.overview.overviewUnsaved = false;
		},
		inputTeamSettingPermissionTvfLimit: function () {
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		clickTeamSettingPermissionAddTokenLimit: function () {
			var limitId = new Date().getTime();
			this.teamSettingAttrs.overview.tokenTeamLimitList.push({
				limitId: limitId,
				tokenId: '',
				tokenIdEdit: '',
				token: null,
				tokenEdit: null,
				tokenLimit: 0,
				tokenLimitText: '',
				limitType: {
					value: 2
				},
				enabled: {
					value: 1
				},
				enabledEdit: {
					value: 1
				},
				deleted: false
			});
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		clickTeamSettingPermissionDeleteTokenLimit: function (limitId) {
			for (var i = 0; i < this.teamSettingAttrs.overview.tokenTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.tokenTeamLimitList[i];
				if (ilimit.limitId == limitId) {
					ilimit.deleted = true;
					break;
				}
			}
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		clickTeamSettingPermissionTokenList: function () {
			this.teamSettingAttrs.overview.tokenListDropdown = !this.teamSettingAttrs.overview.tokenListDropdown;
			window.event.stopPropagation();
		},
		clickTeamSettingPermissionTokenItem: function (limitId, tokenId) {
			var limit = null;
			this.teamSettingAttrs.overview.tokenTeamLimitList.forEach(function (ilimit) {
				if (ilimit.limitId == limitId) {
					limit = ilimit;
					return false;
				}
			});
			var token = null;
			this.tokenList.forEach(function (itoken) {
				if (itoken.tokenId == tokenId) {
					token = itoken;
					return false;
				}
			});
			if (limit && token && limit.tokenId != token.tokenId) {
				limit.tokenIdEdit = token.tokenId;
				limit.tokenEdit = token;
				this.teamSettingAttrs.overview.permissionUnsaved = true;
			}
		},
		inputTeamSettingPermissionTokenLimit: function () {
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		switchTeamSettingPermissionTokenLimitEnabled: function () {
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		clickTeamSettingPermissionAddNftLimit: function () {
			var limitId = new Date().getTime();
			this.teamSettingAttrs.overview.nftTeamLimitList.push({
				limitId: limitId,
				nftContract: '',
				nftContractText: '',
				limitType: {
					value: 4
				},
				enabled: {
					value: 1
				},
				enabledEdit: {
					value: 1
				},
				deleted: false
			});
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		clickTeamSettingPermissionDeleteNftLimit: function (limitId) {
			for (var i = 0; i < this.teamSettingAttrs.overview.nftTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.nftTeamLimitList[i];
				if (ilimit.limitId == limitId) {
					ilimit.deleted = true;
					break;
				}
			}
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		inputTeamSettingPermissionNftContract: function () {
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		switchTeamSettingPermissionNftLimitEnabled: function () {
			this.teamSettingAttrs.overview.permissionUnsaved = true;
		},
		clickTeamSettingSavePermission: function () {
			var teamLimitList = [];
			var tvfLimit = (Number(this.teamSettingAttrs.overview.tvfLimitText) || 0) * 100;
			if (tvfLimit > 0) {
				teamLimitList.push({
					limitId: this.teamSettingAttrs.overview.tvfTeamLimit ? this.teamSettingAttrs.overview.tvfTeamLimit.limitId : '',
					tvfLimit: tvfLimit,
					limitType: 1,
					enabled: 1
				});
			}
			this.teamSettingAttrs.overview.tokenTeamLimitList.forEach(function (ilimit) {
				if (ilimit.deleted) {
					return;
				}
				teamLimitList.push({
					limitId: typeof(ilimit.limitId) == 'number' ? '' : ilimit.limitId,
					tokenId: ilimit.tokenIdEdit,
					tokenLimit: Number(ilimit.tokenLimitText) * 100 || 0,
					limitType: 2,
					enabled: ilimit.enabledEdit.value ? 1 : 0
				});
			});
			this.teamSettingAttrs.overview.nftTeamLimitList.forEach(function (ilimit) {
				if (ilimit.deleted) {
					return;
				}
				teamLimitList.push({
					limitId: typeof(ilimit.limitId) == 'number' ? '' : ilimit.limitId,
					nftContract: ilimit.nftContractText,
					limitType: 4,
					enabled: ilimit.enabledEdit.value ? 1 : 0
				});
			});
			Api.saveTeamLimitList(this, {
				teamId: this.team.teamId,
				teamLimitList: teamLimitList
			}, function (rspBody) {
				this.teamSettingAttrs.overview.permissionUnsaved = false;
				this.loadTeamLimitListByTeam(this.team.teamId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickTeamSettingResetPermission: function () {
			this.teamSettingAttrs.overview.tvfLimitText = this.teamSettingAttrs.overview.tvfTeamLimit ? String(this.teamSettingAttrs.overview.tvfTeamLimit / 100) : '';
			for (var i = 0; i < this.teamSettingAttrs.overview.tokenTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.tokenTeamLimitList[i];
				if (typeof (ilimit.limitId) == 'number') {
					this.teamSettingAttrs.overview.tokenTeamLimitList.splice(i, 1);
					i--;
				}
				else {
					ilimit.tokenIdEdit = ilimit.tokenId;
					ilimit.tokenEdit = ilimit.token;
					ilimit.tokenLimitText = ilimit.tokenLimit / 100;
					ilimit.enabledEdit.value = ilimit.enabled.value;
					ilimit.deleted = false;
				}
			}
			for (var i = 0; i < this.teamSettingAttrs.overview.nftTeamLimitList.length; i++) {
				var ilimit = this.teamSettingAttrs.overview.nftTeamLimitList[i];
				if (typeof (ilimit.limitId) == 'number') {
					this.teamSettingAttrs.overview.nftTeamLimitList.splice(i, 1);
					i--;
				}
				else {
					ilimit.nftContractText = ilimit.nftContract;
					ilimit.enabledEdit.value = ilimit.enabled.value;
					ilimit.deleted = false;
				}
			}
			this.teamSettingAttrs.overview.permissionUnsaved = false;
		},
		clickTeamSettingCancel: function () {
			this.teamSetting = false;
		},
		clickDomainUserSetting: function () {
			this.domainUserSetting = true;
			window.event.stopPropagation();
		},
		clickDomainUserSettingCancel: function () {
			this.domainUserSetting = false;
		},
		clickLeaveDomain: function () {
			Api.leaveDomain(this, {
				domainId: this.domain.domainId
			}, function (rspBody) {
				this.loadUserDomainList(function () {
					Url.hash('id', '');
					var domainId = '';
					if (this.domainList.length > 0) {
						domainId = this.domainList[0].domainId;
					}
					if (domainId) {
						window.location = '/server/' + Url.encode(domainId);
						return;
						this.loadDomain(domainId, function () {
							if (this.domain) {
								this.loadDomainTeamList(domainId, function () {
									if (this.teamList.length) {
										this.team = this.teamList[0];
										this.teamSettingAttrs.overview.name = this.team.name;
										this.teamSettingAttrs.overview.about = this.team.about;
										this.teamSettingAttrs.overview.adminOnly = this.team.adminOnly.value;
										this.loadTeamMessageList(this.team.domainId, this.team.teamId, null, null, function () {
											this.msgListScroll();
											
											this.loadTeamMessageNotices();
										});
										this.loadTeamLimitListByTeam(this.team.teamId);
									}
									else {
										this.team = null;
										this.teamSettingAttrs.overview.name = '';
										this.teamSettingAttrs.overview.about = '';
										this.teamSettingAttrs.overview.adminOnly = 0;
										this.teamSettingAttrs.overview.tvfLimitText = '';
										this.teamSettingAttrs.overview.tvfTeamLimit = null;
										this.teamSettingAttrs.overview.tokenTeamLimitList = [];
										this.teamSettingAttrs.overview.nftTeamLimitList = [];
										this.messageList = [];
										
										this.loadTeamMessageNotices();
									}
								});
								this.loadDomainUserList(domainId);
							}
							else {
								this.categoryList = [];
								this.teamList = [];
								this.team = null;
								this.teamSettingAttrs.overview.name = '';
								this.teamSettingAttrs.overview.about = '';
								this.teamSettingAttrs.overview.adminOnly = 0;
								this.teamSettingAttrs.overview.tvfLimitText = '';
								this.teamSettingAttrs.overview.tvfTeamLimit = null;
								this.teamSettingAttrs.overview.tokenTeamLimitList = [];
								this.teamSettingAttrs.overview.nftTeamLimitList = [];
								this.users = new Array();
								this.userList = [];
								this.domainRoleList = [];
								this.domainRoleUserList = [];
								this.messageList = [];
								
								this.loadTeamMessageNotices();
							}
						});
					}
					else {
						window.location = '/home.html';
						return;
						this.domain = null;
						this.domainList = [];
						this.categoryList = [];
						this.teamList = [];
						this.team = null;
						this.teamSettingAttrs.overview.name = '';
						this.teamSettingAttrs.overview.about = '';
						this.teamSettingAttrs.overview.adminOnly = 0;
						this.teamSettingAttrs.overview.tvfLimitText = '';
						this.teamSettingAttrs.overview.tvfTeamLimit = null;
						this.teamSettingAttrs.overview.tokenTeamLimitList = [];
						this.teamSettingAttrs.overview.nftTeamLimitList = [];
						this.users = new Array();
						this.userList = [];
						this.domainRoleList = [];
						this.domainRoleUserList = [];
						this.messageList = [];
						
						this.loadTeamMessageNotices();
					}
				});
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
				this.loadTeamMessageList(this.domain.domainId, this.team.teamId, null, (this.messageList.length ? this.messageList[0].messageId : null), function () {
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
				this.clickDomain(domainId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickMsgReact: function (messageId, emoji) {
			Api.reactTeamMessage(this, {
				messageId: messageId,
				emojiContent: emoji
			}, function (rspBody) {
				this.messageList.forEach(function (imsg) {
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
			Api.reactTeamMessage(this, {
				messageId: this.reactMessage.messageId,
				emojiContent: emoji
			}, function (rspBody) {
				this.reactMessage.reactionList = rspBody.message.reactionList;
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickMessageOperReact: function (messageId) {
			this.replyMessage = null;
			this.editMessage = null;
			this.messageList.forEach(function (imessage) {
				if (imessage.messageId == messageId) {
					this.reactMessage = imessage;
					return false;
				}
			}, this);
			var width = window.innerWidth;
			var height = window.innerHeight;
			var x = window.event.pageX - window.event.offsetX;
			var y = window.event.pageY - window.event.offsetY;
			if (height - y < 260 + 10) {
				this.reactPanelTop = height - 260 - 10;
			}
			else {
				this.reactPanelTop = y - 10;
			}
			this.reactPanelRight = width - x + 10;
			this.showReactEmoji = !this.showReactEmoji;
			window.event.stopPropagation();
		},
		clickMessageOperReply: function (messageId) {
			this.reactMessage = null;
			this.editMessage = null;
			this.messageList.forEach(function (imessage) {
				if (imessage.messageId == messageId) {
					this.replyMessage = imessage;
					return false;
				}
			}, this);
			this.$el.querySelector('.msg .send .inputs-holder .textbox').focus();
		},
		clickMessageOperReplyCancel: function () {
			this.replyMessage = null;
		},
		clickMessageOperEdit: function (messageId) {
			this.reactMessage = null;
			this.replyMessage = null;
			this.messageList.forEach(function (imessage) {
				if (imessage.messageId == messageId) {
					this.editMessage = imessage;
					return false;
				}
			}, this);
			var textbox = this.$el.querySelector('.msg .send .inputs-holder .textbox');
			textbox.innerText = this.editMessage.textContent;
			textbox.focus();
		},
		clickMessageOperEditCancel: function () {
			this.editMessage = null;
		},
		clickMessageOperRevoke: function (messageId) {
			Api.revokeTeamMessage(this, {
				messageId: messageId
			}, function (rspBody) {
				for (var i = 0; i < this.messageList.length; i++) {
					var imessage = this.messageList[i];
					if (imessage.messageId == messageId) {
						this.messageList.splice(i, 1);
						break;
					}
				}
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickMessageOperRemove: function (messageId) {
			Api.removeTeamMessage(this, {
				messageId: messageId
			}, function (rspBody) {
				for (var i = 0; i < this.messageList.length; i++) {
					var imessage = this.messageList[i];
					if (imessage.messageId == messageId) {
						this.messageList.splice(i, 1);
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
				Api.sendTeamMessage(this, {
					teamId: this.team.teamId,
					messageType: messageType,
					url: result.url,
					replyMessageId: this.replyMessage ? this.replyMessage.messageId : ''
				}, function (rspBody) {
					this.reactMessage = null;
					this.replyMessage = null;
					this.editMessage = null;
				}, function (code, message) {
					MsgBox.alert(message);
				});
			}, function (message) {
				MsgBox.alert(message);
			});
		},
		keydownMessage: function (e) {
			this.messaging = e.keyCode;
			if (e.keyCode == 13 && !e.shiftKey) {
				e.preventDefault();
			}
		},
		keyupMessage: function (e) {
			var textContent = e.target.innerText;
			if (e.keyCode != 13 || e.shiftKey || this.messaging != e.keyCode) {
				return;
			}
			if (!textContent) {
				return;
			}
			if (!this.domain) {
				//alert('Create Island First.');
				return;
			}
			if (!this.editMessage) {
				Api.sendTeamMessage(this, {
					teamId: this.team.teamId,
					messageType: 1,
					textContent: textContent,
					replyMessageId: this.replyMessage ? this.replyMessage.messageId : ''
				}, function (rspBody) {
					e.target.innerText = '';
					this.reactMessage = null;
					this.replyMessage = null;
					this.editMessage = null;
				}, function (code, message) {
					e.target.innerText = '';
					MsgBox.alert(message);
				});
			}
			else {
				Api.editTeamMessage(this, {
					messageId: this.editMessage.messageId,
					textContent: textContent
				}, function (rspBody) {
					e.target.innerText = '';
					this.editMessage.textContent = rspBody.message.textContent;
					this.editMessage = null;
				}, function (code, message) {
					e.target.innerText = '';
					MsgBox.alert(message);
				});
			}
		},
		clickNotificationInactiveDropdown: function () {
			this.userSettingAttrs.notification.inactiveDropdown = !this.userSettingAttrs.notification.inactiveDropdown;
			window.event.stopPropagation();
		},
		clickServerViewInactiveTeamDropdown: function () {
			this.domainSettingAttrs.serverview.inactiveTeamDropdown = !this.domainSettingAttrs.serverview.inactiveTeamDropdown;
			window.event.stopPropagation();
		},
		clickServerViewInactiveTimeDropdown: function () {
			this.domainSettingAttrs.serverview.inactiveTimeDropdown = !this.domainSettingAttrs.serverview.inactiveTimeDropdown;
			window.event.stopPropagation();
		},
		clickServerViewSystemMessageDropdown: function () {
			this.domainSettingAttrs.serverview.systemMessageDropdown = !this.domainSettingAttrs.serverview.systemMessageDropdown;
			window.event.stopPropagation();
		},
		clickCommviewRuleTeamDropdown: function () {
			this.domainSettingAttrs.commview.ruleTeamDropdown = !this.domainSettingAttrs.commview.ruleTeamDropdown;
			window.event.stopPropagation();
		},
		clickCommviewUpdateTeamDropdown: function () {
			this.domainSettingAttrs.commview.updateTeamDropdown = !this.domainSettingAttrs.commview.updateTeamDropdown;
			window.event.stopPropagation();
		},
		clickCommviewLanguageDropdown: function () {
			this.domainSettingAttrs.commview.languageDropdown = !this.domainSettingAttrs.commview.languageDropdown;
			window.event.stopPropagation();
		},
		clickMembersRoleDropdown: function () {
			this.domainSettingAttrs.members.roleDropdown = !this.domainSettingAttrs.members.roleDropdown;
			window.event.stopPropagation();
		},
		clickMembersRoleDropdownRole: function (roleId) {
			this.domainRoleList.forEach(function (irole) {
				if (irole.roleId == roleId) {
					this.domainSettingAttrs.members.selectRole = irole;
					return false;
				}
			}, this);
		},
		clickKickDomainUser: function (userId) {
			Api.kickDomainUser(this, {
				domainId: this.domain.domainId,
				memberId: userId
			}, function (rspBody) {
				this.loadDomainUserList(this.domain.domainId);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		},
		clickTalkEmoji: function () {
			this.showTalkEmoji = !this.showTalkEmoji;
			window.event.stopPropagation();
		},
		clickTalkEmojiItem: function (emoji) {
			var textbox = document.querySelector('.send .inputs-holder .textbox');
			textbox.focus();
			textbox.innerText += emoji;
			this.showTalkEmoji = false;
		},
		clickViewImageClose: function () {
			this.viewImage = false;
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
					domainNotice.teamNotices.forEach(function (teamNotice) {
						var nteam = null;
						this.teamList.forEach(function (iteam) {
							if (iteam.teamId == teamNotice.teamId) {
								nteam = iteam;
								return false;
							}
						});
						if (nteam) {
							nteam.noticeCount = teamNotice.count;
						}
					}, this);
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
		loadUserWhisperList: function (onsuccess, onfailed) {
			Api.loadUserWhisperList(this, null, function (rspBody) {
				this.whisperList = rspBody.whisperList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadUserDomainList: function (onsuccess, onfailed) {
			Api.loadUserDomainList(this, null, function (rspBody) {
				rspBody.domainList.forEach(function (idomain) {
					idomain.noticeCount = 0;
				});
				this.domainList = rspBody.domainList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadDomain: function (domainId, onsuccess, onfailed) {
			if (!domainId) {
				return;
			}
			Api.loadDomain(this, {
				domainId: domainId
			}, function (rspBody) {
				this.domain = rspBody.domain;
				this.domainSettingAttrs.serverview.domainName = this.domain ? this.domain.name : '';
				this.domainSettingAttrs.serverview.domainAddress = this.domain ? this.domain.address : '';
				this.domainSettingAttrs.serverview.domainAbout = this.domain ? this.domain.about : '';
				this.domainSettingAttrs.serverview.domainAvatar = this.domain ? this.domain.avatar : '';
				this.domainSettingAttrs.serverview.domainBanner = this.domain ? this.domain.banner : '';
				this.domainSettingAttrs.serverview.domainVisibility = (this.domain && this.domain.visibility.value == 2);
				this.domainSettingAttrs.serverview.unsaved = false;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadDomainTeamList: function (domainId, onsuccess, onfailed) {
			if (!domainId) {
				return;
			}
			Api.loadDomainTeamList(this, {
				domainId: domainId
			}, function (rspBody) {
				var categorys = new Array();
				rspBody.categoryList.forEach(function (category) {
					categorys[category.categoryId] = category;
				});
				rspBody.teamList.forEach(function (team) {
					team.category = categorys[team.categoryId] || null;
					team.noticeCount = 0;
				});
				this.categoryList = rspBody.categoryList;
				this.teamList = rspBody.teamList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadDomainUserList: function (domainId, onsuccess, onfailed) {
			if (!domainId) {
				return;
			}
			Api.loadDomainUserList(this, {
				domainId: domainId
			}, function (rspBody) {
				var users = new Array();
				rspBody.userList.forEach(function (user) {
					user.roleList = [];
					user.addRoleUserSelected =  false;
					users[user.userId] = user;
				}, this);
				var domainRoles = new Array();
				var editRole = null;
				rspBody.domainRoleList.forEach(function (domainRole) {
					domainRole.userList = [];
					domainRoles[domainRole.roleId] = domainRole;
					if (this.domainSettingAttrs.rolesview.role && domainRole.roleId == this.domainSettingAttrs.rolesview.role.roleId) {
						editRole = domainRole;
					}
					if (!editRole && domainRole.isdefault.value == 1) {
						editRole = domainRole;
					}
					if (!this.domainSettingAttrs.members.selectRole && domainRole.isdefault.value == 1) {
						this.domainSettingAttrs.members.selectRole = domainRole;
					}
				}, this);
				this.domainSettingAttrs.rolesview.privilegeViewTeam = false;
				this.domainSettingAttrs.rolesview.privilegeAdminTeam = false;
				this.domainSettingAttrs.rolesview.privilegeAdminRole = false;
				this.domainSettingAttrs.rolesview.privilegeAdminDomain = false;
				this.domainSettingAttrs.rolesview.privilegeInviteMember = false;
				this.domainSettingAttrs.rolesview.privilegeRemoveMember = false;
				this.domainSettingAttrs.rolesview.privilegeSendMessage = false;
				if (editRole) {
					this.domainSettingAttrs.rolesview.role = editRole;
					this.domainSettingAttrs.rolesview.roleNameEdit = editRole.name;
					this.domainSettingAttrs.rolesview.roleColorEdit = editRole.color;
					editRole.privileges.forEach(function (privilege) {
						if (privilege == 1) {
							this.domainSettingAttrs.rolesview.privilegeViewTeam = true;
						}
						else if (privilege == 2) {
							this.domainSettingAttrs.rolesview.privilegeAdminTeam = true;
						}
						else if (privilege == 4) {
							this.domainSettingAttrs.rolesview.privilegeAdminRole = true;
						}
						else if (privilege == 8) {
							this.domainSettingAttrs.rolesview.privilegeAdminDomain = true;
						}
						else if (privilege == 12) {
							this.domainSettingAttrs.rolesview.privilegeInviteMember = true;
						}
						else if (privilege == 14) {
							this.domainSettingAttrs.rolesview.privilegeRemoveMember = true;
						}
						else if (privilege == 22) {
							this.domainSettingAttrs.rolesview.privilegeSendMessage = true;
						}
					}, this);
				}
				rspBody.domainRoleUserList.forEach(function (domainRoleUser) {
					var user = users[domainRoleUser.userId];
					var domainRole = domainRoles[domainRoleUser.roleId];
					if (user && domainRole && !domainRole.isdefault.value) {
						user.roleList.push(domainRole);
						domainRole.userList.push(user);
					}
				}, this);
				
				this.users = users;
				this.userList = rspBody.userList;
				this.domainRoleList = rspBody.domainRoleList;
				this.domainRoleUserList = rspBody.domainRoleUserList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadTeamMessageList: function (domainId, teamId, fromId, toId, onsuccess, onfailed) {
			if (!teamId) {
				return;
			}
			Api.loadTeamMessageList(this, {
				teamId: teamId,
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
					this.messageList = this.messageList.concat(rspBody.messageList);
				}
				else if (toId) {
					this.messageList = rspBody.messageList.concat(this.messageList);
				}
				else {
					this.messageList = rspBody.messageList;
				}
				onsuccess && onsuccess.call(this);
				
				Api.acceptTeamMessageNotice(this, {
					teamId: teamId
				});
				this.domainNotices.forEach(function (domainNotice) {
					if (domainNotice.domainId == domainId) {
						domainNotice.teamNotices.forEach(function (teamNotice) {
							if (teamNotice.teamId == teamId) {
								domainNotice.count = (domainNotice.count >= teamNotice.count ? domainNotice.count - teamNotice.count : 0);
								teamNotice.count = 0;
								return false;
							}
						});
						return false;
					}
				});
				var deltaCount = 0;
				this.teamList.forEach(function (iteam) {
					if (iteam.teamId == teamId) {
						deltaCount = iteam.noticeCount;
						iteam.noticeCount = 0;
						return false;
					}
				});
				if (deltaCount > 0) {
					this.domainList.forEach(function (idomain) {
						if (idomain.domainId == domainId) {
							idomain.noticeCount = (idomain.noticeCount >= deltaCount ? idomain.noticeCount - deltaCount : 0);
							return false;
						}
					});
				}
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		loadTeamLimitListByTeam: function (teamId, onsuccess, onfailed) {
			if (!teamId) {
				return;
			}
			Api.loadTeamLimitListByTeam(this, {
				teamId: teamId
			}, function (rspBody) {
				var tokens = new Array();
				rspBody.tokens.forEach(function (itoken) {
					tokens[itoken.tokenId] = itoken;
				});
				var tvfLimit = null;
				var tokenLimitList = [];
				var nftLimitList = [];
				rspBody.teamLimitList.forEach(function (ilimit) {
					if (ilimit.limitType.value == 1) {
						if (!tvfLimit) {
							tvfLimit = ilimit;
						}
					}
					else if (ilimit.limitType.value == 2) {
						ilimit.token = tokens[ilimit.tokenId];
						if (ilimit.token) {
							ilimit.tokenIdEdit = ilimit.tokenId;
							ilimit.tokenEdit = ilimit.token;
							ilimit.tokenLimitText = String(ilimit.tokenLimit / 100 || '');
							tokenLimitList.push(ilimit);
						}
					}
					else if (ilimit.limitType.value == 4) {
						ilimit.nftContractText = ilimit.nftContract;
						nftLimitList.push(ilimit);
					}
					ilimit.enabledEdit = {
						value: ilimit.enabled.value
					};
					ilimit.permit = true;
					ilimit.deleted = false;
				});
				this.teamSettingAttrs.overview.tvfLimitText = tvfLimit ? (tvfLimit.tvfLimit / 100 || '') : '';
				this.teamSettingAttrs.overview.tvfTeamLimit = tvfLimit;
				this.teamSettingAttrs.overview.tokenTeamLimitList = tokenLimitList;
				this.teamSettingAttrs.overview.nftTeamLimitList = nftLimitList;
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
		loadTokenList: function (onsuccess, onfailed) {
			Api.loadTokenList(this, null, function (rspBody) {
				this.tokenList = rspBody.tokenList;
				onsuccess && onsuccess.call(this);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		joinDomain: function (domainId, onsuccess, onfailed) {
			Api.joinDomain(this, {
				domainId: domainId
			}, function (rspBody) {
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
		createCategory: function (onsuccess, onfailed) {
			Api.createCategory(this, {
				domainId: this.domain.domainId,
				name: this.addCategory.name
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.category);
			}, function (code, message) {
				MsgBox.alert(message);
				onfailed && onfailed.call(this, code, message);
			});
		},
		createTeam: function (onsuccess, onfailed) {
			Api.createTeam(this, {
				domainId: this.domain.domainId,
				categoryId: this.addTeam.categoryId,
				name: this.addTeam.name,
				about: this.addTeam.about
			}, function (rspBody) {
				onsuccess && onsuccess.call(this, rspBody.team);
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