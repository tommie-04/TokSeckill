const providers = [
  { id: 101, name: "通用文本模型" },
  { id: 102, name: "推理模型" },
  { id: 103, name: "多模态模型" }
];

new Vue({
  el: "#catalog",
  data: {
    packs: [],
    loading: true,
    catalogError: false,
    submitting: null,
    checking: false,
    orderId: null,
    message: "",
    signedIn: Boolean(sessionStorage.getItem("token"))
  },
  mounted() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.catalogError = false;
      const results = await Promise.allSettled(providers.map(async provider => {
        const { data } = await axios.get("/token-packs/list/" + provider.id);
        return data.map(pack => ({ ...pack, provider: provider.name }));
      }));
      this.catalogError = results.some(result => result.status === "rejected");
      this.packs = results.filter(result => result.status === "fulfilled").flatMap(result => result.value);
      this.loading = false;
    },
    formatUnits(units) {
      return Number(units).toLocaleString("zh-CN");
    },
    formatPrice(cents) {
      return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(cents / 100);
    },
    isOpen(pack) {
      const now = Date.now();
      return Date.parse(pack.beginTime + "Z") <= now && now < Date.parse(pack.endTime + "Z");
    },
    async reserve(pack) {
      if (!this.signedIn) {
        location.href = "/login.html";
        return;
      }
      this.submitting = pack.id;
      this.orderId = null;
      this.message = "";
      try {
        const { data } = await axios.post("/token-pack-orders/seckill/" + pack.id, null, { timeout: 18000 });
        this.orderId = data;
        await this.checkOrder();
      } catch (error) {
        this.message = String(error);
        const unconfirmed = /订单号：(\d+)/.exec(this.message);
        if (unconfirmed) {
          this.orderId = unconfirmed[1];
        }
      } finally {
        this.submitting = null;
      }
    },
    async checkOrder() {
      this.checking = true;
      try {
        const { data } = await axios.get("/token-pack-orders/" + this.orderId);
        this.message = data.status === "CREATED" ? "订单已创建。" : "暂未查到订单，可稍后刷新。";
      } catch (error) {
        this.message = String(error);
      } finally {
        this.checking = false;
      }
    },
    signOut() {
      sessionStorage.removeItem("token");
      location.reload();
    }
  }
});
