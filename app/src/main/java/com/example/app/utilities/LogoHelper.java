package com.example.app.utilities;

import com.example.app.R;

public class LogoHelper {

    /**
     * Returns the drawable resource ID for a given website domain.
     * 
     * @param website The website URL or name (e.g., "google.com", "GitHub")
     * @return The resource ID of the logo, or 0 if no match is found.
     */
    public static int getLogoResource(String website) {
        if (website == null || website.isEmpty()) {
            return 0;
        }

        String lowerWebsite = website.toLowerCase();

        if (lowerWebsite.contains("github")) {
            return R.drawable.logo_github;
        } else if (lowerWebsite.contains("instagram")) {
            return R.drawable.logo_instagram;
        } else if (lowerWebsite.contains("twitter") || lowerWebsite.equals("x.com")) {
            return R.drawable.logo_twitter;
        } else if (lowerWebsite.contains("youtube")) {
            return R.drawable.logo_youtube;
        } else if (lowerWebsite.contains("netflix")) {
            return R.drawable.logo_netflix;
        }else if (lowerWebsite.contains("google")) {
            return R.drawable.logo_google;
        }else if (lowerWebsite.contains("linkedin")) {
            return R.drawable.logo_linkedin;
        }else if (lowerWebsite.contains("facebook")) {
            return R.drawable.logo_facebook;
        }else if (lowerWebsite.contains("railone")) {
            return R.drawable.logo_railone;
        }else if (lowerWebsite.contains("telegram")) {
            return R.drawable.logo_telegram;
        }else if (lowerWebsite.contains("spotify")) {
            return R.drawable.logo_spotify;
        }

        return 0;
    }
}
