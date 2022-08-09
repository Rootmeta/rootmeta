var MsgBox = (function () {
	var _onok = null;
	var _onyes = null;
	var _onno = null;
	var _Class = function () {
		this.alert = function (content, onok) {
			_onok = onok || null;
			document.querySelector('.msg-box.alert .content .msg .body').innerText = content;
			document.querySelector('.msg-box.alert').classList.add('show');
		};
		this.confirm = function (content, onyes, onno) {
			_onyes = onyes || null;
			_onno = onno || null;
			document.querySelector('.msg-box.confirm .content .msg .body').innerText = content;
			document.querySelector('.msg-box.confirm').classList.add('show');
		};
		this.hide = function () {
			document.querySelector('.msg-box.alert').classList.remove('show');
			_onok && _onok();
			_onok = null;
		}
		this.yes = function () {
			document.querySelector('.msg-box.confirm').classList.remove('show');
			_onyes && _onyes();
			_onyes = null;
			_onno = null;
		}
		this.no = function () {
			document.querySelector('.msg-box.confirm').classList.remove('show');
			_onno && _onno();
			_onyes = null;
			_onno = null;
		}
	};
	return new _Class();
})();
var Store = (function () {
	var _Class = function () {
		this.set = function (key, value) {
			if (!key) {
				return;
			}
			localStorage.setItem(key, value);
		};
		this.get = function (key) {
			if (!key) {
				return null;
			}
			return localStorage.getItem(key);
		}
	};
	return new _Class();
})();
var Cookie = (function () {
	var _Class = function () {
		this.set = function (key, value) {
			if (!key) {
				return;
			}
			var expire = new Date();
			expire.setTime(expire.getTime() + 86400 * 1000 * 7);
			document.cookie = key + '=' + escape(value) + ';expires=' + expire.toGMTString() + ';path=/';
		};
		this.get = function (key) {
			if (!key) {
				return null;
			}
			key = key + '=';
			var ks = document.cookie.indexOf(key);
			while (ks >= 0) {
				var vs = ks + key.length;
				var ve = document.cookie.indexOf(';', vs);
				if (ve < 0) {
					ve = document.cookie.length;
				}
				var value = unescape(document.cookie.substring(vs, ve));
				if (value) {
					return value;
				}
				ks = document.cookie.indexOf(key, ve);
			}
			return null;
		}
	};
	return new _Class();
})();
var Url = (function () {
	var _Class = function () {
		this.encode = function (text) {
			return text ? encodeURIComponent(text) : '';
		};
		this.decode = function (text) {
			return text ? decodeURIComponent(text) : '';
		};
		this.parameter = function (name) {
			var results = location.search.match(new RegExp('[\?\&]' + name + '=([^\&]*)', 'i'));
			return results ? results[1] : '';
		};
		this.hash = function (name, value) {
			if (arguments.length == 0) {
				return '';
			}
			if (!name) {
				return '';
			}
			if (arguments.length == 1) {
				var results = location.hash.match(new RegExp('[\#\&]' + name + '=([^\&]*)', 'i'));
				return results ? results[1] : '';
			}
			var hash = location.hash;
			var results = hash.match(new RegExp('[\#\&]' + name + '=([^\&]*)', 'i'));
			if (results == null) {
				location.hash += (!hash ? '#' : '&') + name + '=' + value;
			}
			else {
				location.hash = hash.replace(results[0], results[0].charAt(0) + name + '=' + value);
			}
		};
	};
	return new _Class();
})();
var Util = (function () {
	var _Class = function () {
		this.formatAmount = function (amount) {
			return amount.toFixed(2).replace(/\d(?=(?:\d{3})+\b)/g, '$&,');
		};
	};
	return new _Class();
})();
var Hexer = (function () {
	var _Class = function () {
		this.encode = function (text) {
			var utf8 = [];
			for (var i = 0; i < text.length; i++) {
				var charcode = text.charCodeAt(i);
				if (charcode < 0x80) {
					utf8.push(charcode);
				}
				else if (charcode < 0x800) {
					utf8.push(0xc0 | (charcode >> 6), 0x80 | (charcode & 0x3f));
				}
				else if (charcode < 0xd800 || charcode >= 0xe000) {
					utf8.push(0xe0 | (charcode >> 12), 0x80 | ((charcode>>6) & 0x3f), 0x80 | (charcode & 0x3f));
				}
				else {
					i++;
					charcode = 0x10000 + (((charcode & 0x3ff) << 10) | (text.charCodeAt(i) & 0x3ff));
					utf8.push(0xf0 | (charcode >>18), 0x80 | ((charcode>>12) & 0x3f), 0x80 | ((charcode>>6) & 0x3f), 0x80 | (charcode & 0x3f));
				}
			}
			var hex = utf8.map(n => n.toString(16));
			return '0x' + hex.join('');
		};
	};
	return new _Class();
})();
var Api = (function () {
	var URL = "https://api.rootmeta.xyz";
	var _Class = function () {
		this.ajax = function (svc, cmd, _this, reqBody, success, failure) {
			axios.post(URL, {
				head: {
					svc: svc || '',
					cmd: cmd || '',
					lang: Store.get('locale') || 'en',
					platform: 'WEB',
					client: Store.get('client') || '',
					series: String(new Date().getTime()),
					uticket: Store.get('uticket') || ''
				},
				body: reqBody || {}
			}).then(function (result) {
				if (result.status != 200) {
					failure && failure.call(_this, result.status, resultText);
					return;
				}
				if (result.data.head.code != 0) {
					if (result.data.head.code == 9 || result.data.head.code == 8) {
						window.location = '/index.html?redirect=' + Url.encode(window.location.href);
					}
					else {
						failure && failure.call(_this, result.data.head.code, result.data.head.message);
					}
					return;
				}
				success && success.call(_this, result.data.body);
			});
		};
	};
	return new _Class();
})();

//base
Api.loadTagList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_tag_list', _this, reqBody, success, failure);
};
Api.loadTokenList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_token_list', _this, reqBody, success, failure);
};
//account
Api.client = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'client', _this, reqBody, success, failure);
};
Api.unique = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'unique', _this, reqBody, success, failure);
};
Api.sign = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'sign', _this, reqBody, success, failure);
};
Api.swap = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'swap', _this, reqBody, success, failure);
};
Api.init = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'init', _this, reqBody, success, failure);
};
Api.edit = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'edit', _this, reqBody, success, failure);
};
Api.quit = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'quit', _this, reqBody, success, failure);
};
Api.loadUserList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_list', _this, reqBody, success, failure);
};
Api.loadUser = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user', _this, reqBody, success, failure);
};
Api.loadUserInfo = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_info', _this, reqBody, success, failure);
};
Api.loadUserNftList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_nft_list', _this, reqBody, success, failure);
};
//talk
Api.exploreDomainList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'explore_domain_list', _this, reqBody, success, failure);
};
Api.inviteDomainByFriend = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'invite_domain_by_friend', _this, reqBody, success, failure);
};
Api.inviteDomainByWhisper = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'invite_domain_by_whisper', _this, reqBody, success, failure);
};
Api.inviteDomainByDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'invite_domain_by_domain', _this, reqBody, success, failure);
};
Api.kickDomainUser = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'kick_domain_user', _this, reqBody, success, failure);
};
Api.joinDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'join_domain', _this, reqBody, success, failure);
};
Api.leaveDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'leave_domain', _this, reqBody, success, failure);
};
Api.boostDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'boost_domain', _this, reqBody, success, failure);
};
Api.buildDomainPoster = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'build_domain_poster', _this, reqBody, success, failure);
};
Api.loadUserDomainList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_domain_list', _this, reqBody, success, failure);
};
Api.loadUserMutualDomains = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_mutual_domains', _this, reqBody, success, failure);
};
Api.loadDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_domain', _this, reqBody, success, failure);
};
Api.loadDomainTeamList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_domain_team_list', _this, reqBody, success, failure);
};
Api.loadDomainRoleList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_domain_role_list', _this, reqBody, success, failure);
};
Api.loadDomainUserList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_domain_user_list', _this, reqBody, success, failure);
};
Api.loadTeamMessageList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_team_message_list', _this, reqBody, success, failure);
};
Api.loadTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_team_message', _this, reqBody, success, failure);
};
Api.saveDomainRole = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'save_domain_role', _this, reqBody, success, failure);
};
Api.addDomainRoleUser = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'add_domain_role_user', _this, reqBody, success, failure);
};
Api.removeDomainRoleUser = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'remove_domain_role_user', _this, reqBody, success, failure);
};
Api.createDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'create_domain', _this, reqBody, success, failure);
};
Api.editDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'edit_domain', _this, reqBody, success, failure);
};
Api.deleteDomain = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'delete_domain', _this, reqBody, success, failure);
};
Api.createCategory = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'create_category', _this, reqBody, success, failure);
};
Api.editCategory = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'edit_category', _this, reqBody, success, failure);
};
Api.deleteCategory = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'delete_category', _this, reqBody, success, failure);
};
Api.createTeam = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'create_team', _this, reqBody, success, failure);
};
Api.editTeam = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'edit_team', _this, reqBody, success, failure);
};
Api.deleteTeam = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'delete_team', _this, reqBody, success, failure);
};
Api.loadTeamLimitListByTeam = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_team_limit_list_by_team', _this, reqBody, success, failure);
};
Api.saveTeamLimitList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'save_team_limit_list', _this, reqBody, success, failure);
};
Api.sendTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'send_team_message', _this, reqBody, success, failure);
};
Api.editTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'edit_team_message', _this, reqBody, success, failure);
};
Api.reactTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'react_team_message', _this, reqBody, success, failure);
};
Api.replyTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'reply_team_message', _this, reqBody, success, failure);
};
Api.revokeTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'revoke_team_message', _this, reqBody, success, failure);
};
Api.removeTeamMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'remove_team_message', _this, reqBody, success, failure);
};
Api.loadTeamMessageNotices = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_team_message_notices', _this, reqBody, success, failure);
};
Api.acceptTeamMessageNotice = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'accept_team_message_notice', _this, reqBody, success, failure);
};
//chat
Api.loadUserFriendList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_friend_list', _this, reqBody, success, failure);
};
Api.loadUserMutualFriends = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_mutual_friends', _this, reqBody, success, failure);
};
Api.inviteUserFriend = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'invite_user_friend', _this, reqBody, success, failure);
};
Api.cancelUserFriend = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'cancel_user_friend', _this, reqBody, success, failure);
};
Api.acceptUserFriend = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'accept_user_friend', _this, reqBody, success, failure);
};
Api.loadUserFriendNotice = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_friend_notice', _this, reqBody, success, failure);
};
Api.acceptUserFriendNotice = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'accept_user_friend_notice', _this, reqBody, success, failure);
};
Api.rejectUserFriend = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'reject_user_friend', _this, reqBody, success, failure);
};
Api.removeUserFriend = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'remove_user_friend', _this, reqBody, success, failure);
};
Api.loadUserWhisperList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_user_whisper_list', _this, reqBody, success, failure);
};
Api.promiseWhisper = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'promise_whisper', _this, reqBody, success, failure);
};
Api.createWhisper = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'create_whisper', _this, reqBody, success, failure);
};
Api.deleteWhisper = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'delete_whisper', _this, reqBody, success, failure);
};
Api.loadWhisperUserList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_whisper_user_list', _this, reqBody, success, failure);
};
Api.joinWhisperUsers = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'join_whisper_users', _this, reqBody, success, failure);
};
Api.kickWhisperUsers = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'kick_whisper_users', _this, reqBody, success, failure);
};
Api.enterWhisperUser = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'enter_whisper_user', _this, reqBody, success, failure);
};
Api.leaveWhisperUser = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'leave_whisper_user', _this, reqBody, success, failure);
};
Api.loadWhisperMessageList = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_whisper_message_list', _this, reqBody, success, failure);
};
Api.loadWhisperMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_whisper_message', _this, reqBody, success, failure);
};
Api.sendWhisperMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'send_whisper_message', _this, reqBody, success, failure);
};
Api.editWhisperMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'edit_whisper_message', _this, reqBody, success, failure);
};
Api.reactWhisperMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'react_whisper_message', _this, reqBody, success, failure);
};
Api.replyWhisperMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'reply_whisper_message', _this, reqBody, success, failure);
};
Api.revokeWhisperMessage = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'revoke_whisper_message', _this, reqBody, success, failure);
};
Api.loadWhisperMessageNotices = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'load_whisper_message_notices', _this, reqBody, success, failure);
};
Api.acceptWhisperMessageNotice = function (_this, reqBody, success, failure) {
	this.ajax('meta_community', 'accept_whisper_message_notice', _this, reqBody, success, failure);
};

Api.upload = function (_this, file, success, failure) {
	var data = new FormData();
	data.append("file", file);
	axios.post("https://up.rootmeta.xyz", data).then(function (result) {
		if (result.status != 200) {
			failure && failure.call(_this, 'UPLOAD FAILED');
			return;
		}
		if (result.data.state != 'SUCCESS') {
			failure && failure.call(_this, result.data.msg);
			return;
		}
		success && success.call(_this, result.data);
	});
};