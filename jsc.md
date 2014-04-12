---
layout: page
title: "JSC Demo page"
description: "Demonstrates usage of the JSC/Jekyll Scientific plugin"
---
{% include JB/setup %}

Mark down using cite {% cite pagh2005optimal %}, is new but invention x {% cite boom2 %} is awesome. Twas a creation of J's {% cite boom3 %} {% cite kirsch2006distance %}. Do you know what {% m %}e = mc^2{% em %} actually means? But do you really?

What about this ?

{% math %}
\mathbf{V}_1 \times \mathbf{V}_2 =  \begin{vmatrix}
\mathbf{i} & \mathbf{j} & \mathbf{k} \\
\frac{\partial X}{\partial u} &  \frac{\partial Y}{\partial u} & 0 \\
\frac{\partial X}{\partial v} &  \frac{\partial Y}{\partial v} & 0
\end{vmatrix} 
{% endmath %}

## References
{% bibliography -c %}