# Higgs.IO

Abstractions:
http://en.wikipedia.org/wiki/Particle_accelerator
http://en.wikipedia.org/wiki/Particle

Add a Magnet to an Accelerator
Create Beams between Accelerators
Each Magnet can affect every Beam it wants
An Accelerator accelerates either Atoms or Particles along a Beam
A Magnet can pick a particle out of a beam, use/manipulate it and re-accelerate it or destroy it
Accelerators have Receptors which allow creating Beams between multiple accelerators.
Using these receptors, an accelerator can beam particles to other accelerators.
Within an Accelerator there is are Particle Pipelines. Each Beam between accelerator has it's own pipeline.
It is to this pipeline that magnets are added and through which the Beam travels, carrying it's accelerated particles.


An Acceleraator is composed of Magnets.
Create an Accelerator through which you can have multiple Beams.
A Beam can have 0...* Particles
You "accelerate" particles via a Beam
A particle is composed of Atoms.
A Jumbo Particle is a complete particle with all its Atoms.
Atoms can traval on their own and are re-assembled into a particle at the other end of a Beam
