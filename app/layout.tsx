import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Aurora Next — Open-source Android App Store",
  description: "Browse, search, and sideload Android apps privately. Aurora Next is an open-source Google Play alternative with no tracking.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} antialiased bg-gray-50 text-gray-900`}>
        {children}
      </body>
    </html>
  );
}
