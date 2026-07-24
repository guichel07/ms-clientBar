export interface Customer {
  firstname: string;
  lastname: string;
  phone: string;
}

const AVATAR_COLORS = [
  "#B6481E", // clay
  "#3F7D58", // green
  "#C68A1F", // gold
  "#33476C", // ink-line
  "#B23A33", // rust
  "#C95B2C", // clay-bright
  "#283A58", // ink-soft
  "#6B4226", // brun
  "#2E7D6B", // sarcelle
  "#8E5B9A", // violet
  "#1F7A8C", // bleu canard
  "#A6763D", // ocre
];

export interface ClientbarInterface {
  customers: Customer[];
  // Reçoit uniquement le client sélectionné (ou nouvellement créé), pour que
  // main.ts reste synchronisé avec l'état interne de ClientBar.
  callback?: (customer: Customer) => void;
  selectedCustomer?: (customer: Customer) => void;
}

export class ClientBar {
  private el: HTMLElement;
  private overlayEl: HTMLDivElement;
  private newClientOverlayEl: HTMLDivElement;

  // ---- état client (porté depuis le pattern CUSTOMERS / selectCustomer
  // de l'app admin) : une liste de clients, un terme de recherche, et le
  // client actuellement sélectionné pour la vente en cours.
  private customers: Customer[] = [];
  private clientSearchTerm = "";
  private selectedCustomer: Customer | null = null;
  private callback?: (customer: Customer) => void;
  private setCustomerSelected?: (customer: Customer) => void;

  constructor(mountPoint: HTMLElement) {
    this.el = mountPoint;

    // Les overlays (sheet client + formulaire nouveau client) ne dépendent plus
    // de divs placeholder devant exister "par magie" dans index.html. On les
    // crée nous-mêmes une seule fois et on les attache au body : plus jamais
    // de querySelector qui renvoie null parce que le HTML statique a oublié
    // le conteneur.
    this.overlayEl = document.createElement("div");
    this.overlayEl.id = "client-overlay";
    document.body.appendChild(this.overlayEl);

    this.newClientOverlayEl = document.createElement("div");
    this.newClientOverlayEl.id = "new-client-overlay";
    document.body.appendChild(this.newClientOverlayEl);
  }

  render(clientInterface: ClientbarInterface): void {
    this.customers = clientInterface.customers;
    this.callback = clientInterface.callback;
    this.setCustomerSelected = clientInterface.selectedCustomer;

    this.el.innerHTML = `
      <div id="client-bar" style="display:flex; align-items:center; gap:10px; background:#fff; border:1px solid var(--paper-line); border-radius:13px; padding:11px 14px; margin-bottom:12px;">
        <div class="avatar" id="client-bar-avatar" style="width:34px; height:34px; font-size:11px; flex-shrink:0;">?</div>
        <div style="flex:1; min-width:0;">
          <div id="client-bar-name" style="font-size:11.5px; font-weight:700; color:var(--ink);">Aucun client sélectionné</div>
          <div id="client-bar-sub" style="font-size:9.5px; color:var(--ink-muted); margin-top:1px;">Touchez pour identifier la cliente</div>
        </div>
        <span style="font-size:10px; font-weight:700; color:var(--clay); flex-shrink:0;">Choisir ›</span>
      </div>
    `;

    // Référence directe à l'élément qu'on vient de créer : jamais de
    // querySelector fragile juste après une injection innerHTML.
    const bar = this.el.firstElementChild as HTMLElement | null;
    if (!bar) return;

    bar.addEventListener("click", () => {
      this.renderClientSheet();
    });

    this.updateBarDisplay();
  }

  // Reflète this.selectedCustomer dans la barre (avatar, nom, sous-texte).
  // Appelée après render() et à chaque sélection/désélection de client.
  private updateBarDisplay(): void {
    const avatar = this.el.querySelector<HTMLElement>("#client-bar-avatar");
    const name = this.el.querySelector<HTMLElement>("#client-bar-name");
    const sub = this.el.querySelector<HTMLElement>("#client-bar-sub");
    if (!avatar || !name || !sub) return;

    if (this.selectedCustomer) {
      avatar.textContent = this.getCustomerInitials(this.selectedCustomer.firstname, this.selectedCustomer.lastname);
      avatar.style.background = AVATAR_COLORS[this.customers.indexOf(this.selectedCustomer) % AVATAR_COLORS.length];
      name.textContent = `${this.selectedCustomer.firstname} ${this.selectedCustomer.lastname}`;
      sub.textContent = this.selectedCustomer.phone;
      this.setCustomerSelected?.(this.selectedCustomer);
    } else {
      avatar.textContent = "?";
      avatar.style.background = "";
      name.textContent = "Aucun client sélectionné";
      sub.textContent = "Touchez pour identifier la cliente";
    }
  }

  private getCustomerInitials(firstname: string, lastname: string): string {
    return ((firstname[0] ?? "") + (lastname[0] ?? "")).toUpperCase();
  }

  renderClientSheet(): void {
    this.clientSearchTerm = "";

    this.overlayEl.innerHTML = `
      <!-- ============================= SHEET CLIENT ============================= -->
      <div class="overlay show">
        <div class="sheet" style="max-height:88vh;">
          <div class="sheet-handle"></div>
          <h3>Identifier la cliente</h3>
          <div class="catalog-price" style="margin-bottom:10px;">Recherchez par nom ou numéro</div>

          <div class="search-input-wrap" id="client-search-wrap" style="margin-bottom:12px;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.4" y2="16.4"/></svg>
            <input type="text" id="client-search-input" placeholder="Nom ou téléphone...">
            <button class="search-clear" id="client-search-clear">✕</button>
          </div>

          <div class="clients-grid" id="client-results-list" style="grid-template-columns:1fr; max-height:38vh; overflow-y:auto; margin-bottom:14px;"></div>

          <button class="btn-confirm" style="width:100%;" id="nouveau-client">+ Nouveau client</button>
        </div>
      </div>
    `;

    this.overlayEl
      .querySelector<HTMLButtonElement>("#nouveau-client")
      ?.addEventListener("click", () => this.renderClientOverlay());

    this.overlayEl
      .querySelector<HTMLButtonElement>("#client-search-clear")
      ?.addEventListener("click", () => this.clearClientSearch());

    const searchInput = this.overlayEl.querySelector<HTMLInputElement>("#client-search-input");
    const searchWrap = this.overlayEl.querySelector<HTMLElement>("#client-search-wrap");
    searchInput?.addEventListener("focus", () => searchWrap?.classList.add("focus"));
    searchInput?.addEventListener("blur", () => searchWrap?.classList.remove("focus"));
    searchInput?.addEventListener("input", () => {
      this.clientSearchTerm = searchInput.value.toLowerCase();
      this.renderClientResults();
    });

    // Ferme le sheet si on clique sur le fond assombri (en dehors de la carte)
    this.overlayEl.querySelector(".overlay")?.addEventListener("click", (e) => {
      if (e.target === e.currentTarget) this.resetClientSheet();
    });

    // Affiche la liste complète dès l'ouverture, pas seulement après une frappe.
    this.renderClientResults();
  }

  // Porté depuis renderClientResults() de l'app admin : même logique de
  // filtrage (nom complet OU téléphone, espaces ignorés), mais rendu ici
  // avec des listeners directs plutôt qu'un onclick="selectCustomer(...)"
  // inline (qui exigerait une fonction globale sur window).
  private renderClientResults(): void {
    const term = this.clientSearchTerm;
    const filtered = !term
      ? this.customers
      : this.customers.filter(
          (c) =>
            `${c.firstname} ${c.lastname}`.toLowerCase().includes(term) ||
            c.phone.replace(/\s/g, "").includes(term.replace(/\s/g, ""))
        );

    const list = this.overlayEl.querySelector<HTMLElement>("#client-results-list");
    if (!list) return;

    if (filtered.length === 0) {
      list.innerHTML = `<div class="no-results"><span class="mark">Aucun client</span>Créez une nouvelle fiche ci-dessous</div>`;
      return;
    }

    list.innerHTML = filtered
      .map(
        (c, idx) => `
      <div class="client-card" style="cursor:pointer;" data-phone="${c.phone}">
        <div class="avatar" style="background:${AVATAR_COLORS[idx % AVATAR_COLORS.length]};">${this.getCustomerInitials(c.firstname, c.lastname)}</div>
        <div class="info">
          <div class="cname">${c.firstname} ${c.lastname}</div>
          <div class="cphone">${c.phone}</div>
        </div>
      </div>
    `
      )
      .join("");

    // Un seul listener délégué sur la liste plutôt qu'un onclick par carte :
    // évite de re-attacher N listeners à chaque frappe de recherche.
    list.querySelectorAll<HTMLElement>(".client-card").forEach((card) => {
      card.addEventListener("click", () => {
        const phone = card.dataset.phone;
        if (phone) this.selectCustomer(phone);
      });
    });
  }

  private selectCustomer(phone: string): void {
    const customer = this.customers.find((c) => c.phone === phone);
    if (!customer) return;
    this.selectedCustomer = customer;
    this.updateBarDisplay();
    this.resetClientSheet();
  }

  resetClientSheet(): void {
    this.overlayEl.innerHTML = ``;
  }

  renderClientOverlay(): void {
    this.newClientOverlayEl.innerHTML = `
      <!-- ============================= SHEET NOUVEAU CLIENT ============================= -->
      <div class="overlay show">
        <div class="sheet">
          <div class="sheet-handle"></div>
          <h3>Nouveau client</h3>
          <div class="catalog-price" style="margin-bottom:10px;">Enregistré pour vos prochaines ventes</div>

          <div class="field"><label>Prénom</label><input type="text" id="new-client-firstname"></div>
          <div id="new-client-firstname-error" style="display:none; color:var(--rust); font-size:9.5px; font-weight:600; margin:-8px 0 10px;">
            Le prénom doit contenir au moins 3 caractères
          </div>
          <div class="field"><label>Nom</label><input type="text" id="new-client-lastname"></div>
          <div id="new-client-lastname-error" style="display:none; color:var(--rust); font-size:9.5px; font-weight:600; margin:-8px 0 10px;">
            Le nom doit contenir au moins 3 caractères
          </div>
          <div class="field" style="margin-bottom:0;">
            <label>Téléphone</label>
            <input type="tel" id="new-client-phone" placeholder="+242 7X XXX XX XX" value="+242 ">
          </div>
          <div id="new-client-phone-error" style="display:none; color:var(--rust); font-size:9.5px; font-weight:600; margin-top:5px;">
            Format invalide — attendu +242 7X XXX XX XX
          </div>

          <div class="sheet-actions" style="margin-top:14px;">
            <button class="btn-cancel" id="cancel-new-client">Annuler</button>
            <button class="btn-confirm" id="save-new-client">Enregistrer</button>
          </div>
        </div>
      </div>
    `;

    this.newClientOverlayEl
      .querySelector<HTMLButtonElement>("#cancel-new-client")
      ?.addEventListener("click", () => this.resetClientOverlay());

    const firstnameInput = this.newClientOverlayEl.querySelector<HTMLInputElement>("#new-client-firstname");
    const firstnameError = this.newClientOverlayEl.querySelector<HTMLElement>("#new-client-firstname-error");
    firstnameInput?.addEventListener("input", () => {
      if (firstnameError) firstnameError.style.display = "none";
    });

    const lastnameInput = this.newClientOverlayEl.querySelector<HTMLInputElement>("#new-client-lastname");
    const lastnameError = this.newClientOverlayEl.querySelector<HTMLElement>("#new-client-lastname-error");
    lastnameInput?.addEventListener("input", () => {
      if (lastnameError) lastnameError.style.display = "none";
    });

    // Formatage en direct : "+242 " reste fixe, les chiffres tapés ensuite
    // sont regroupés automatiquement en "7X XXX XX XX" (groupes 2-3-2-2).
    const PHONE_PREFIX = "+242 ";
    const phoneInput = this.newClientOverlayEl.querySelector<HTMLInputElement>("#new-client-phone");
    const phoneError = this.newClientOverlayEl.querySelector<HTMLElement>("#new-client-phone-error");

    // Empêche le curseur/la sélection de se placer AVANT la fin du préfixe.
    // Sinon un caractère tapé avant "+242 " casse le regroupement des chiffres.
    const clampPhoneSelection = () => {
      if (!phoneInput) return;
      const start = phoneInput.selectionStart ?? 0;
      const end = phoneInput.selectionEnd ?? 0;
      if (start < PHONE_PREFIX.length || end < PHONE_PREFIX.length) {
        const pos = phoneInput.value.length;
        phoneInput.setSelectionRange(pos, pos);
      }
    };
    phoneInput?.addEventListener("click", clampPhoneSelection);
    phoneInput?.addEventListener("keyup", clampPhoneSelection);

    phoneInput?.addEventListener("input", () => {
      let raw = phoneInput.value;
      // Filet de sécurité : si le préfixe a quand même été abîmé (sélection
      // totale effacée, collage par-dessus, etc.), on le reconstruit avant
      // d'extraire les chiffres, plutôt que de laisser le format partir en vrille.
      if (!raw.startsWith("+242")) {
        raw = PHONE_PREFIX + raw.replace(/\D/g, "");
      }
      const digits = this.extractLocalPhoneDigits(raw);
      phoneInput.value = PHONE_PREFIX + this.formatLocalPhoneDigits(digits);
      phoneInput.setSelectionRange(phoneInput.value.length, phoneInput.value.length);
      if (phoneError) phoneError.style.display = "none";
    });
    phoneInput?.addEventListener("focus", () => {
      if (!phoneInput.value) phoneInput.value = PHONE_PREFIX;
      clampPhoneSelection();
    });

    this.newClientOverlayEl
      .querySelector<HTMLButtonElement>("#save-new-client")
      ?.addEventListener("click", () => {
        // On interroge this.newClientOverlayEl (là où vivent réellement ces
        // inputs), pas this.el qui pointe vers #app / la barre client.
        // On ne s'arrête plus au premier champ invalide : on vérifie tout,
        // on affiche chaque indicateur rouge concerné, puis on bloque.
        let hasError = false;

        const firstname = firstnameInput?.value ?? "";
        if (firstname.length < 3) {
          if (firstnameError) firstnameError.style.display = "block";
          hasError = true;
        } else if (firstnameError) {
          firstnameError.style.display = "none";
        }

        const lastname = lastnameInput?.value ?? "";
        if (lastname.length < 3) {
          if (lastnameError) lastnameError.style.display = "block";
          hasError = true;
        } else if (lastnameError) {
          lastnameError.style.display = "none";
        }

        const phone = phoneInput?.value ?? "";
        const localDigits = this.extractLocalPhoneDigits(phone);
        if (localDigits.length !== 9) {
          if (phoneError) phoneError.style.display = "block";
          hasError = true;
        } else if (phoneError) {
          phoneError.style.display = "none";
        }

        if (hasError) return;

        // Évite les doublons : si ce numéro existe déjà, on réutilise la
        // fiche existante plutôt que d'en créer une seconde.
        const existing = this.customers.find((c) => c.phone === phone);
        const newClient: Customer = existing ?? { firstname, lastname, phone };
        if (!existing) this.customers.unshift(newClient);

        // Le client qu'on vient de créer devient automatiquement le client
        // sélectionné pour la vente en cours — évite d'avoir à le rechercher
        // à nouveau juste après l'avoir saisi.
        this.selectedCustomer = newClient;
        this.updateBarDisplay();

        this.resetClientOverlay();
        this.resetClientSheet();

        // Notifie main.ts avec le client créé (ou la fiche existante réutilisée).
        this.callback?.(newClient);
      });
  }

  // Retire tout ce qui n'est pas un chiffre, puis retire l'indicatif "242"
  // s'il est présent en tête, pour ne garder que les 9 chiffres du numéro local.
  private extractLocalPhoneDigits(raw: string): string {
    let digits = raw.replace(/\D/g, "");
    if (digits.startsWith("242")) digits = digits.slice(3);
    return digits.slice(0, 9);
  }

  // Regroupe les chiffres locaux en "7X XXX XX XX" (groupes de 2-3-2-2).
  private formatLocalPhoneDigits(digits: string): string {
    const groups = [2, 3, 2, 2];
    let out = "";
    let i = 0;
    for (const size of groups) {
      if (i >= digits.length) break;
      out += (out ? " " : "") + digits.slice(i, i + size);
      i += size;
    }
    return out;
  }

  resetClientOverlay(): void {
    this.newClientOverlayEl.innerHTML = ``;
  }

  private clearClientSearch(): void {
    const input = this.overlayEl.querySelector<HTMLInputElement>("#client-search-input");
    if (input) input.value = "";
    this.clientSearchTerm = "";
    this.renderClientResults();
  }
}
