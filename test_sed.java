    private List<String> getSavedFolders() {
        List<String> res = new ArrayList<>();
        String json = sp.getString("folders", "[]");
        try {
