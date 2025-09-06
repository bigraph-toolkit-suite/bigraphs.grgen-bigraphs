FROM ubuntu:22.04

# Set environment variables for non-interactive installation
ENV DEBIAN_FRONTEND=noninteractive

# Update and install dependencies
RUN apt update && apt install -y \
    wget \
    curl \
    unzip \
    xauth  \
    xorg \
    xvfb \
    git \
    openjdk-17-jdk \
    openjdk-11-jdk \
    maven \
    && rm -rf /var/lib/apt/lists/*

# Switch to the root user
USER root
# Change shell
SHELL ["/bin/bash", "-c"]


## jEnv Setup
# Install jEnv
RUN git clone https://github.com/jenv/jenv.git ~/.jenv
# Add jEnv to the shell profile
ENV PATH="~/.jenv/bin:$PATH"
# The interactive shell sources .bashrc (instead of bash_profile)
RUN echo 'export PATH="~/.jenv/bin:$PATH"' >> ~/.bashrc
RUN echo 'eval "$(jenv init -)"' >> ~/.bashrc
# Have JAVA_HOME get set by jEnv and get it loaded in the bash
RUN /bin/bash -c 'source ~/.bashrc && eval "$(jenv init -)" && jenv enable-plugin export'
RUN /bin/bash -c 'source ~/.bashrc && exec $SHELL -l'
# Set up jEnv for both Java versions; configure global one
RUN /bin/bash -c "source ~/.bashrc && jenv add /usr/lib/jvm/java-17-openjdk-amd64" \
    && /bin/bash -c "source ~/.bashrc && jenv add /usr/lib/jvm/java-11-openjdk-amd64" \
    && /bin/bash -c "source ~/.bashrc && jenv global 17"


## GrGen.NET Setup
RUN mkdir -p /workspace
RUN wget -O /workspace/GrGenNET-V7.2.1-2025-04-22.zip https://grgen.de/GrGenNET-V7.2.1-2025-04-22.zip
RUN mkdir /opt/grgen \
    && cd /workspace/ && unzip GrGenNET-V7.2.1-2025-04-22.zip \
    && mv GrGenNET-V7.2.1-2025-04-22/* /opt/grgen/ \
    && rmdir GrGenNET-V7.2.1-2025-04-22 \
    && rm -f GrGenNET-V7.2.1-2025-04-22.zip \
    && cd /opt/grgen/bin \
    && /bin/bash -c "source ~/.bashrc && jenv local 11"
ENV PATH="/opt/grgen/bin:$PATH"


## Mono Setup
RUN apt update && apt install -y ca-certificates gnupg \
    && gpg --homedir /tmp --no-default-keyring --keyring /usr/share/keyrings/mono-official-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF \
    && echo "deb [signed-by=/usr/share/keyrings/mono-official-archive-keyring.gpg] https://download.mono-project.com/repo/ubuntu stable-focal main" | tee /etc/apt/sources.list.d/mono-official-stable.list \
    && apt update \
    && apt install -y mono-devel


## BiGGer Setup
# Clone repository, check out latest version tag, and build the tool
RUN git clone https://github.com/bigraph-toolkit-suite/bigraphs.grgen-bigraphs.git \
    && cd bigraphs.grgen-bigraphs \
    && git checkout tags/v1.3.0 \
    && /bin/bash -c "source ~/.bashrc && jenv local 17" \
    && /bin/bash -c "source ~/.bashrc && mvn clean package -PfatJar -DskipTests" \
RUN /bin/bash -c "source ~/.bashrc && echo 'chmod +x /bigraphs.grgen-bigraphs/bin/bigger.jar' >> ~/.bashrc"
# Add generated bigger.jar to the ENV path
ENV PATH="/bigraphs.grgen-bigraphs/bin:$PATH"


# Set up the working directory
WORKDIR /bigraphs.grgen-bigraphs/bin
# Set the default command to run bash
CMD [ "/bin/bash", "-c" ]