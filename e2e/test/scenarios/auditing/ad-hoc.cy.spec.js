import { restore, describeEE, openNativeEditor, startNewQuestion } from "e2e/support/helpers";

describeEE("audit > ad-hoc", () => {
  describe("native query", () => {
    beforeEach(() => {
      restore();

      cy.log("Run ad hoc native query as normal user");
      cy.signInAsNormalUser();

      openNativeEditor().type("SELECT 123");
      cy.icon("play").first().click();

      // make sure results rendered before moving forward
      cy.get(".ScalarValue").contains("123");

      // Sign in as admin to be able to access audit logs in tests
      cy.signInAsAdmin();
    });

    it("should appear in audit log (metabase#16845 metabase-enterprise#486)", () => {
      cy.visit("/admin/audit/members/log");

      cy.findByText("Native")
        .parent()
        .parent()
        .within(() => {
          cy.findByText("Ad-hoc").click();
        });

      cy.log("Assert that the query page is not blank");
      cy.url().should("include", "/admin/audit/query/");

      cy.get(".PageTitle").contains("Query");
      cy.findByText("Open in Metabase");
      cy.get(".ace_content").contains("SELECT 123");
    });
  });

  describe("notebook query", () => {
    beforeEach(() => {
      cy.intercept('/api/dataset').as('dataset');

      restore();

      cy.log("Run ad hoc notebook query as normal user");
      cy.signInAsNormalUser();
      startNewQuestion();
      cy.findByTextEnsureVisible("Sample Database").click();
      cy.findByTextEnsureVisible("Orders").click();

      cy.button('Visualize').click();

      cy.wait('@dataset');
      // Sign in as admin to be able to access audit logs in tests
      cy.signInAsAdmin();
    });

    it.skip("should appear in audit log #29456", () => {
      cy.visit("/admin/audit/members/log");

      cy.findByText("GUI")
        .parent()
        .parent()
        .within(() => {
          cy.findByText("Ad-hoc").click();
        });

      cy.log("Assert that the query page is not blank");
      cy.url().should("include", "/admin/audit/query/");

      cy.get(".PageTitle").contains("Query");
      cy.findByText("Open in Metabase");
    });
  });
});
