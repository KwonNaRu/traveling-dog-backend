import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "@/styles/reset.scss"; // Reset styles
import "@/styles/variables.scss";
import "@/styles/mixins.scss";
import "./globals.scss";

const geistSans = Geist({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const geistMono = Geist_Mono({
    variable: "--font-geist-mono",
    subsets: ["latin"],
});

export const metadata: Metadata = {
    title: "Traveling Dog",
    description: "AI-powered travel planner",
};

export default function RootLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="ko-KR">
            <body className={`${geistSans.variable} ${geistMono.variable}`}>{children}</body>
        </html>
    );
}
