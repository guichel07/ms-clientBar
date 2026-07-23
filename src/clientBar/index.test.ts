import { describe, it, expect, beforeEach, vi } from "vitest";
import { ClientBar, type Customer } from ".";

describe("ClientBar", () => {
  let container: HTMLDivElement;
  let clientBar: ClientBar;

  const customers: Customer[] = [
    {
      firstname: "Jean",
      lastname: "Dupont",
      phone: "06 00 00 00 00",
    },
    {
      firstname: "Marie",
      lastname: "Curie",
      phone: "07 00 00 00 00",
    },
  ];

  beforeEach(() => {
    document.body.innerHTML = "";
    container = document.createElement("div");
    document.body.appendChild(container);

    clientBar = new ClientBar(container);
  });

  it("doit afficher la barre client", () => {
    clientBar.render({
      customers,
    });

    expect(container.textContent).toContain("Aucun client sélectionné");
    expect(container.textContent).toContain("Choisir");
  });

  it("ouvre le client sheet lorsqu'on clique sur la barre", () => {
    clientBar.render({
      customers,
    });

    container.querySelector("#client-bar")!.dispatchEvent(
      new MouseEvent("click", { bubbles: true })
    );

    expect(document.querySelector("#client-overlay")!.textContent).toContain(
      "Identifier la cliente"
    );
  });

  it("affiche les clients dans le sheet", () => {
    clientBar.render({
      customers,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    const cards = document.querySelectorAll(".client-card");

    expect(cards.length).toBe(2);
  });

  it("filtre les clients avec la recherche", () => {
    clientBar.render({
      customers,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    const input = document.querySelector(
      "#client-search-input"
    ) as HTMLInputElement;

    input.value = "Jean";
    input.dispatchEvent(new Event("input"));

    const cards = document.querySelectorAll(".client-card");

    expect(cards.length).toBe(1);
    expect(cards[0].textContent).toContain("Jean");
  });

  it("sélectionne un client et déclenche le callback", () => {
    const callback = vi.fn();

    clientBar.render({
      customers,
      callback,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    const firstCard = document.querySelector(".client-card") as HTMLElement;

    firstCard.click();

    expect(container.textContent).toContain("Jean Dupont");
  });

  it("ouvre le formulaire nouveau client", () => {
    clientBar.render({
      customers,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    (
      document.querySelector("#nouveau-client") as HTMLButtonElement
    ).click();

    expect(document.querySelector("#new-client-overlay")!.textContent).toContain(
      "Nouveau client"
    );
  });

  it("affiche les erreurs si le formulaire est vide", () => {
    clientBar.render({
      customers,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    (
      document.querySelector("#nouveau-client") as HTMLButtonElement
    ).click();

    (
      document.querySelector("#save-new-client") as HTMLButtonElement
    ).click();

    expect(
      (
        document.querySelector(
          "#new-client-firstname-error"
        ) as HTMLElement
      ).style.display
    ).toBe("block");

    expect(
      (
        document.querySelector(
          "#new-client-lastname-error"
        ) as HTMLElement
      ).style.display
    ).toBe("block");

    expect(
      (
        document.querySelector(
          "#new-client-phone-error"
        ) as HTMLElement
      ).style.display
    ).toBe("block");
  });

  it("crée un nouveau client valide", () => {
    const callback = vi.fn();

    clientBar.render({
      customers,
      callback,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    (
      document.querySelector("#nouveau-client") as HTMLButtonElement
    ).click();

    (
      document.querySelector("#new-client-firstname") as HTMLInputElement
    ).value = "Paul";

    (
      document.querySelector("#new-client-lastname") as HTMLInputElement
    ).value = "Martin";

    const phone = document.querySelector(
      "#new-client-phone"
    ) as HTMLInputElement;

    phone.value = "+242 77 123 45 67";
    phone.dispatchEvent(new Event("input"));

    (
      document.querySelector("#save-new-client") as HTMLButtonElement
    ).click();

    expect(callback).toHaveBeenCalledTimes(1);
    expect(container.textContent).toContain("Paul Martin");
  });

  it("vide la recherche lorsqu'on clique sur le bouton clear", () => {
    clientBar.render({
      customers,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    const input = document.querySelector(
      "#client-search-input"
    ) as HTMLInputElement;

    input.value = "Jean";
    input.dispatchEvent(new Event("input"));

    (
      document.querySelector("#client-search-clear") as HTMLButtonElement
    ).click();

    expect(input.value).toBe("");
    expect(document.querySelectorAll(".client-card").length).toBe(3);
  });

  it("ferme le sheet lorsqu'on clique sur le fond", () => {
    clientBar.render({
      customers,
    });

    (container.querySelector("#client-bar") as HTMLElement).click();

    const overlay = document.querySelector(".overlay") as HTMLElement;

    overlay.dispatchEvent(new MouseEvent("click", { bubbles: true }));

    expect(document.querySelector("#client-overlay")!.innerHTML).toBe("");
  });
});
