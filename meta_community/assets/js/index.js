var app = new Vue({
	el: '#app',
	i18n: new VueI18n({
		locale: Store.get('locale') || 'en',
		messages: {
			en: en,
			zh: zh
		}
	}),
	data: {},
	mounted: async function () {
		if (!Store.get('client')) {
			Api.client(this, null, function (rspBody) {
				Store.set('client', rspBody.client);
			}, function (code, message) {
				MsgBox.alert(message);
			});
		}
	},
	methods: {
		temp: function () {
			if (this.$i18n.locale == 'zh') {
				this.$i18n.locale = 'en';
			}
			else {
				this.$i18n.locale = 'zh';
			}
		},
		clickLogin: async function () {
			const accounts = await ethereum.request({
				method: 'eth_requestAccounts'
			});
			const account = accounts[0];
			
			Api.unique(this, {
				address: account
			}, async function (rspBody) {
				var address = rspBody.address;
				var nonce = rspBody.nonce;
				
				const signature = await ethereum.request({
					method: 'personal_sign',
					params: [Hexer.encode(nonce), address]
				});
				
				Api.sign(this, {
					address: address,
					signature: signature
				}, function (rspBody) {
					var uticket = rspBody.uticket;
					Store.set('uticket', uticket);
					
					window.location = Url.decode(Url.parameter('redirect')) || '/home.html';
				}, function (code, message) {
					MsgBox.alert(message);
				});
			}, function (code, message) {
				MsgBox.alert(message);
			});
		}
	}
});