import { render, screen } from "@testing-library/react";
import Page from "../page";

describe("App Page", () => {
    it("renders the correct heading", () => {
        render(<Page />);
        const heading = screen.getByRole("heading", {
            name: /hello, traveling dog!/i,
        });
        expect(heading).toBeTruthy();
    });
});
