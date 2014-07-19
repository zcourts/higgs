package io.higgs.http.server.transformers.handlebars;

import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.base.Charsets;
import io.higgs.core.ResolvedFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsTemplateSource implements TemplateSource {
    private final ResolvedFile file;
    private String data = "";

    public HiggsTemplateSource(ResolvedFile file) {
        this.file = file;
    }

    public String content() throws IOException {
        if (data == null || data.isEmpty()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getStream(), Charsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            data = builder.toString();
        }
        return data;
    }

    @Override
    public String filename() {
        return file.getName();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HiggsTemplateSource that = (HiggsTemplateSource) o;

        if (data != null ? !data.equals(that.data) : that.data != null) {
            return false;
        }
        if (file != null ? !file.equals(that.file) : that.file != null) {
            return false;
        }

        return true;
    }
}

